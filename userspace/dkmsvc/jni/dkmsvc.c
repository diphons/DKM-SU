/*
 * dkmsvc.c
 * Native rewrite of the provided shell script for Android (root-only daemon).
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dirent.h>
#include <stdbool.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <time.h>

#define VERSION "1.6"
#define MAX_BUFFER 8192
#define MAX_LINE 256

#ifndef PROP_VALUE_MAX
#define PROP_VALUE_MAX 92
#endif

#define PKGNAME            "com.diphons.dkmsu"
#define DATA_DIR           "/data/data/"
#define DIR_MAIN           "/data/adb/ksu/bin"
#define FILES_DIR         DATA_DIR PKGNAME "/files"
#define SUSFS_DIR          FILES_DIR "/susfs"
#define SHARED_PREFS         DATA_DIR PKGNAME "/shared_prefs"
#define SETTINGS_PREFS       SHARED_PREFS "/settings.xml"

#define POWER_SUPPLY_DIR   "/sys/class/power_supply/battery"
#define INPUT_SUSPEND      POWER_SUPPLY_DIR "/input_suspend"
#define CHARGE_TYPE        POWER_SUPPLY_DIR "/charge_type"
#define WL_BLOCKER_DIR     "/sys/class/misc/boeffla_wakelock_blocker"
#define WAKEUP_CLASS       "/sys/class/wakeup"
#define OPLUS_SCREEN       "/sys/module/d8g_helper/parameters/oplus_panel_status"

/* Properties used in script */
#define CONFIG           SHARED_PREFS "/configs_service.xml"
#define PROP_VERSION       "version"
#define PROP_PID           "pid"
#define PROP_CMD           "cmd"
#define PROP_EXIT          "exit"
#define PROP_MSG           "msg"
#define PROP_WL            "wl"
#define PROP_TOPAPP           "topapp"
#define PROP_PERAPP           "perapp"
#define PROP_BYPASS_STATUS   "bypass"

#define PERFORMANCE_MONITOR_INTERVAL 30
#define LAG_THRESHOLD 3
#define PSI_STALL_THRESHOLD 40.0  // 40% stall time dianggap lag

/* global state */
static int ongame = 0;
static char focused_app[512] = "not focused";
static char appidrun[128] = "";
static char appidlast[128] = "";
static int lag_detected_count = 0;
static time_t last_perf_check = 0;
static time_t last_restart = 0;
static int restart_count = 0;
const int MAX_RESTARTS_PER_HOUR = 3;

static char *wl_blocker_default = NULL;
static char *wl_blocker_list = NULL;

int mkdirs(const char *dir) {
    char tmp[512];
    char *p = NULL;
    size_t len;

    snprintf(tmp, sizeof(tmp), "%s", dir);
    len = strlen(tmp);
    if (tmp[len - 1] == '/')
        tmp[len - 1] = '\0';

    for (p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = '\0';
            mkdir(tmp, 0777);
            *p = '/';
        }
    }
    return mkdir(tmp, 0777);
}

void init_config() {
    if (access(CONFIG, F_OK) == 0) return;

    mkdirs(SHARED_PREFS);

    FILE *fp = fopen(CONFIG, "w");
    if (!fp) {
        perror("fopen init_config");
        return;
    }

    char default_xml[1024];
    snprintf(default_xml, sizeof(default_xml),
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
        "<map>\n"
        "    <string name=\"version\">v%s</string>\n"
        "    <string name=\"hash\"></string>\n"
        "    <int name=\"pid\" value=\"0\" />\n"
        "    <int name=\"exit\" value=\"0\" />\n"
        "    <string name=\"cmd\"></string>\n"
        "    <string name=\"msg\"></string>\n"
        "    <string name=\"wl\"></string>\n"
        "    <string name=\"topapp\"></string>\n"
        "    <string name=\"perapp\"></string>\n"
        "    <int name=\"bypass\" value=\"0\" />\n"
        "</map>\n",
        VERSION);

    fwrite(default_xml, 1, strlen(default_xml), fp);
    fclose(fp);
}

char *read_value_by_key(const char *key, const char *default_value) {
    FILE *fp = fopen(CONFIG, "r");
    if (!fp) {
        return (char *)default_value;
    }

    static char value[MAX_LINE];
    char line[MAX_LINE];
    
    char string_pattern[128], boolean_pattern[128], int_pattern[128];
    snprintf(string_pattern, sizeof(string_pattern), "<string name=\"%s\">", key);
    snprintf(boolean_pattern, sizeof(boolean_pattern), "<boolean name=\"%s\" value=\"", key);
    snprintf(int_pattern, sizeof(int_pattern), "<int name=\"%s\" value=\"", key);

    while (fgets(line, sizeof(line), fp)) {
        // cek string
        char *pos = strstr(line, string_pattern);
        if (pos) {
            pos += strlen(string_pattern);
            char *end = strstr(pos, "</string>");
            if (end) {
                *end = '\0';
                strncpy(value, pos, sizeof(value) - 1);
                value[sizeof(value) - 1] = '\0';
                fclose(fp);
                return value;
            }
        }

        pos = strstr(line, boolean_pattern);
        if (pos) {
            pos += strlen(boolean_pattern);
            char *end = strchr(pos, '"');
            if (end) {
                *end = '\0';
                strncpy(value, pos, sizeof(value) - 1);
                value[sizeof(value) - 1] = '\0';
                fclose(fp);
                return value;
            }
        }

        // cek int
        pos = strstr(line, int_pattern);
        if (pos) {
            pos += strlen(int_pattern);
            char *end = strchr(pos, '"');
            if (end) {
                *end = '\0';
                strncpy(value, pos, sizeof(value) - 1);
                value[sizeof(value) - 1] = '\0';
                fclose(fp);
                return value;
            }
        }
    }

    fclose(fp);

    strncpy(value, default_value ? default_value : "", sizeof(value) - 1);
    value[sizeof(value) - 1] = '\0';
    return value;
}

void write_key_value(const char *key, const char *value) {
    FILE *file = fopen(CONFIG, "r");
    char *content = NULL;

    if (file) {
        fseek(file, 0, SEEK_END);
        long size = ftell(file);
        rewind(file);
        content = (char *)malloc(size + 1);
        fread(content, 1, size, file);
        content[size] = '\0';
        fclose(file);
    } else {
        content = strdup("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n");
    }

    int value_type = 0;
    
    if (strcmp(value, "true") == 0 || strcmp(value, "false") == 0) {
        value_type = 1;
    } 
    else {
        char *endptr;
        long num = strtol(value, &endptr, 10);
        if (endptr != value && *endptr == '\0') {
            value_type = 2;
        }
    }

    char string_pattern[128], boolean_pattern[128], int_pattern[128];
    snprintf(string_pattern, sizeof(string_pattern), "<string name=\"%s\">", key);
    snprintf(boolean_pattern, sizeof(boolean_pattern), "<boolean name=\"%s\" value=\"", key);
    snprintf(int_pattern, sizeof(int_pattern), "<int name=\"%s\" value=\"", key);

    char *string_pos = strstr(content, string_pattern);
    char *boolean_pos = strstr(content, boolean_pattern);
    char *int_pos = strstr(content, int_pattern);
    
    char *pos = NULL;
    int found_type = -1;
    
    if (string_pos) {
        pos = string_pos;
        found_type = 0;
    } else if (boolean_pos) {
        pos = boolean_pos;
        found_type = 1;
    } else if (int_pos) {
        pos = int_pos;
        found_type = 2;
    }

    char *new_content = (char *)malloc(strlen(content) + strlen(value) + 256);
    new_content[0] = '\0';

    if (pos) {
        if (found_type == 0) {
            char *start = pos + strlen(string_pattern);
            char *end = strstr(start, "</string>");
            if (!end) {
                strcpy(new_content, content);
                pos = NULL;
            } else {
                size_t prefix_len = start - content;
                strncpy(new_content, content, prefix_len);
                new_content[prefix_len] = '\0';
                strcat(new_content, value);
                strcat(new_content, end);
            }
        } else if (found_type == 1) {
            char *start = pos + strlen(boolean_pattern);
            char *end = strstr(start, "\"");
            if (!end) {
                strcpy(new_content, content);
                pos = NULL;
            } else {
                size_t prefix_len = start - content;
                strncpy(new_content, content, prefix_len);
                new_content[prefix_len] = '\0';
                strcat(new_content, value);
                strcat(new_content, end);
            }
        } else if (found_type == 2) {
            char *start = pos + strlen(int_pattern);
            char *end = strstr(start, "\"");
            if (!end) {
                strcpy(new_content, content);
                pos = NULL;
            } else {
                size_t prefix_len = start - content;
                strncpy(new_content, content, prefix_len);
                new_content[prefix_len] = '\0';
                strcat(new_content, value);
                strcat(new_content, end);
            }
        }
    }

    if (!pos) {
        char *insert_point = strstr(content, "</map>");
        if (!insert_point) {
            if (value_type == 1) {
                snprintf(new_content, strlen(value) + 256,
                         "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                         "<map>\n    <boolean name=\"%s\" value=\"%s\" />\n</map>\n", key, value);
            } else if (value_type == 2) {
                snprintf(new_content, strlen(value) + 256,
                         "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                         "<map>\n    <int name=\"%s\" value=\"%s\" />\n</map>\n", key, value);
            } else {
                snprintf(new_content, strlen(value) + 256,
                         "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
                         "<map>\n    <string name=\"%s\">%s</string>\n</map>\n", key, value);
            }
        } else {
            size_t prefix_len = insert_point - content;
            strncpy(new_content, content, prefix_len);
            new_content[prefix_len] = '\0';

            char append_line[256];
            if (value_type == 1) {
                snprintf(append_line, sizeof(append_line),
                         "    <boolean name=\"%s\" value=\"%s\" />\n", key, value);
            } else if (value_type == 2) {
                snprintf(append_line, sizeof(append_line),
                         "    <int name=\"%s\" value=\"%s\" />\n", key, value);
            } else {
                snprintf(append_line, sizeof(append_line),
                         "    <string name=\"%s\">%s</string>\n", key, value);
            }
            
            strcat(new_content, append_line);
            strcat(new_content, insert_point);
        }
    }

    FILE *out = fopen(CONFIG, "w");
    if (out) {
        fputs(new_content, out);
        fclose(out);
    }

    free(content);
    free(new_content);
}

// small helpers
static char *read_file_trim(const char *path) {
    FILE *f = fopen(path, "r");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END);
    long s = ftell(f);
    rewind(f);
    char *buf = malloc(s + 1);
    if (!buf) { fclose(f); return NULL; }
    size_t r = fread(buf, 1, s, f);
    buf[r] = '\0';
    fclose(f);
    while (r > 0 && (buf[r-1] == '\n' || buf[r-1] == '\r' || buf[r-1] == ' ' || buf[r-1] == '\t')) {
        buf[r-1] = '\0';
        r--;
    }
    return buf;
}

/* Utility: run command and capture one-line output (trim newline). Caller gets static buffer - overwrite on each call. */
static char *run_cmd_once(const char *cmd) {
    static char buf[2048];
    FILE *fp = popen(cmd, "r");
    if (!fp) {
        buf[0] = '\0';
        return buf;
    }
    if (!fgets(buf, sizeof(buf), fp)) {
        buf[0] = '\0';
    }
    pclose(fp);
    size_t n = strlen(buf);
    if (n && buf[n-1] == '\n') buf[n-1] = '\0';
    return buf;
}

static char *run_command_capture(const char *cmd) {
    FILE *fp = popen(cmd, "r");
    if (!fp) return NULL;
    char *out = NULL;
    size_t len = 0;
    char buf[1024];
    while (fgets(buf, sizeof(buf), fp)) {
        size_t chunk = strlen(buf);
        char *tmp = realloc(out, len + chunk + 1);
        if (!tmp) { free(out); pclose(fp); return NULL; }
        out = tmp;
        memcpy(out + len, buf, chunk);
        len += chunk;
        out[len] = '\0';
    }
    pclose(fp);
    return out;
}

void execute_command(const char* cmd, char** output) {
    static char buffer[MAX_BUFFER];
    buffer[0] = '\0';
    
    FILE* fp = popen(cmd, "r");
    if (!fp) return;
    
    size_t total_read = 0;
    char temp_buffer[256];
    
    while (fgets(temp_buffer, sizeof(temp_buffer), fp)) {
        size_t len = strlen(temp_buffer);
        if (total_read + len < sizeof(buffer)) {
            strcpy(buffer + total_read, temp_buffer);
            total_read += len;
        } else {
            break;
        }
    }
    
    pclose(fp);
    
    if (output && total_read > 0) {
        *output = strdup(buffer);
    }
}

static void set_prop(const char *key, const char *val) {
    if (!key || !val) return;
    __system_property_set(key, val);
}

static void get_prop(const char *key, char *buf, size_t len) {
    if (!key || !buf) return;
    if (__system_property_get(key, buf) <= 0)
        buf[0] = '\0';
}

static char *read_file_power(const char *path) {
    static char buf[4096];
    buf[0] = '\0';
    FILE *f = fopen(path, "r");
    if (!f) return buf;
    size_t r = fread(buf, 1, sizeof(buf)-1, f);
    buf[r] = '\0';
    fclose(f);
    while (r && (buf[r-1] == '\n' || buf[r-1] == '\r')) { buf[r-1] = '\0'; r--; }
    return buf;
}

static char *read_file_to_buf(const char *path) {
    FILE *f = fopen(path, "r");
    if (!f) return NULL;
    char *buf = malloc(4096);
    if (!buf) { fclose(f); return NULL; }
    size_t r = fread(buf, 1, 4095, f);
    buf[r] = '\0';
    fclose(f);
    while (r && (buf[r-1] == '\n' || buf[r-1] == '\r')) { buf[r-1] = '\0'; r--; }
    return buf;
}

static int file_exists(const char *path) {
	struct stat st;
	return stat(path, &st) == 0;
}

static bool file_contains(const char *path, const char *needle) {
    char *content = read_file_trim(path);
    if (!content) return false;
    bool found = (strstr(content, needle) != NULL);
    free(content);
    return found;
}

static int write_file(const char *path, const char *data) {
    FILE *f = fopen(path, "w");
    if (!f) return -1;
    size_t w = fwrite(data, 1, strlen(data), f);
    fclose(f);
    return (w == strlen(data)) ? 0 : -1;
}

static char *compute_md5(const char *path) {
    char cmd[512];
    snprintf(cmd, sizeof(cmd), "md5sum %s 2>/dev/null | awk '{print $1}'", path);
    return run_cmd_once(cmd);
}

static char *read_sys_file(const char *path) {
    FILE *f = fopen(path, "r");
    if (!f) return strdup("");

    size_t buf_size = 512 * 5;
    size_t len = 0;
    char *buf = malloc(buf_size);
    if (!buf) {
        fclose(f);
        return strdup("");
    }

    size_t n;
    while ((n = fread(buf + len, 1, buf_size - len - 1, f)) > 0) {
        len += n;
        if (len > buf_size - 2) {
            buf_size *= 2;
            char *tmp = realloc(buf, buf_size);
            if (!tmp) {
                free(buf);
                fclose(f);
                return strdup("");
            }
            buf = tmp;
        }
    }

    fclose(f);
    buf[len] = '\0';

    while (len > 0 && (buf[len - 1] == '\n' || buf[len - 1] == '\r' || buf[len - 1] == ' '))
        buf[--len] = '\0';

    return buf;
}

static void replace_semicolon_with_space(char *str) {
    if (!str) return;
    for (; *str; str++) {
        if (*str == ';') *str = ' ';
    }
}

static void get_wl_blocker(void) {
    char path_default[256];
    char path_current[256];
    snprintf(path_default, sizeof(path_default), "%s/wakelock_blocker_default", WL_BLOCKER_DIR);
    snprintf(path_current, sizeof(path_current), "%s/wakelock_blocker", WL_BLOCKER_DIR);

    wl_blocker_default = read_sys_file(path_default);
    if (strlen(wl_blocker_default) == 0) {
        free(wl_blocker_default);
        wl_blocker_default = read_sys_file(path_current);
    }

    replace_semicolon_with_space(wl_blocker_default);

    wl_blocker_list = strdup(wl_blocker_default);
}

/* Unified wakelock JSON generator
 * mode = 0 → print to stdout
 * mode = 1 → write to FILES_DIR/wakelock.json
 */
static void generate_wakelock(int mode) {
    DIR *d = opendir(WAKEUP_CLASS);
    if (!d) {
        perror("opendir /sys/class/wakeup");
        return;
    }

    get_wl_blocker();

    struct {
        char *name;
        char *active;
        char *abortc;
        char *times;
        int block;
    } entries[1024];
    int count = 0;

    struct dirent *de;
    while ((de = readdir(d)) != NULL) {
        if (de->d_name[0] == '.') continue;

        char path_name[512], path_ac[512], path_abort[512], path_times[512];
        snprintf(path_name, sizeof(path_name), "%s/%s/name", WAKEUP_CLASS, de->d_name);
        snprintf(path_ac, sizeof(path_ac), "%s/%s/active_count", WAKEUP_CLASS, de->d_name);
        snprintf(path_abort, sizeof(path_abort), "%s/%s/wakeup_count", WAKEUP_CLASS, de->d_name);
        snprintf(path_times, sizeof(path_times), "%s/%s/total_time_ms", WAKEUP_CLASS, de->d_name);

        char *name   = read_sys_file(path_name);
        char *active = read_sys_file(path_ac);
        char *abortc = read_sys_file(path_abort);
        char *times  = read_sys_file(path_times);

        if (!name[0]) { free(name); free(active); free(abortc); free(times); continue; }

        long times_val = atol(times);
        if (times_val <= 999 && (!wl_blocker_list || !strstr(wl_blocker_list, name))) {
            free(name); free(active); free(abortc); free(times);
            continue;
        }

        int found = 0;
        for (int i = 0; i < count; ++i) {
            if (strcmp(entries[i].name, name) == 0) {
                long old_times = atol(entries[i].times);
                if (times_val > old_times) {
                    free(entries[i].active);
                    free(entries[i].abortc);
                    free(entries[i].times);
                    entries[i].active = active;
                    entries[i].abortc = abortc;
                    entries[i].times = times;
                } else {
                    free(active);
                    free(abortc);
                    free(times);
                }
                free(name);
                found = 1;
                break;
            }
        }

        if (!found && count < 1024) {
            entries[count].name = name;
            entries[count].active = active;
            entries[count].abortc = abortc;
            entries[count].times = times;
            entries[count].block = (wl_blocker_list && strstr(wl_blocker_list, name)) ? 1 : 0;
            count++;
        }
    }
    closedir(d);

    FILE *out = NULL;
    if (mode == 1) {
        char outpath[512];
        snprintf(outpath, sizeof(outpath), "%s/wakelock.json", FILES_DIR);
        out = fopen(outpath, "w");
        if (!out) {
            perror("fopen wakelock.json");
            for (int i = 0; i < count; ++i) {
                free(entries[i].name);
                free(entries[i].active);
                free(entries[i].abortc);
                free(entries[i].times);
            }
            return;
        }
    }

    FILE *dest = (mode == 1) ? out : stdout;
    fprintf(dest, "[\n");
    for (int i = 0; i < count; ++i) {
        if (i > 0) fprintf(dest, ",\n");
        fprintf(dest,
                "  {\n"
                "    \"id\": \"wl%d\",\n"
                "    \"name\": \"%s\",\n"
                "    \"active\": \"%s\",\n"
                "    \"abort\": \"%s\",\n"
                "    \"times\": \"%s\",\n"
                "    \"block\": \"%d\"\n"
                "  }",
                i + 1,
                entries[i].name,
                entries[i].active[0] ? entries[i].active : "0",
                entries[i].abortc[0] ? entries[i].abortc : "0",
                entries[i].times[0] ? entries[i].times : "0",
                entries[i].block);
    }
    fprintf(dest, "\n]\n");

    if (mode == 1) fclose(out);

    for (int i = 0; i < count; ++i) {
        free(entries[i].name);
        free(entries[i].active);
        free(entries[i].abortc);
        free(entries[i].times);
    }

    if (mode == 1) {
        write_key_value(PROP_WL, "0");
    }
}

static void start_battery_broadcast() {
    char cmd[1024];
    snprintf(cmd, sizeof(cmd),
             "am broadcast -a %s.BATTERY_INFO -n %s/.ui.services.BatteryReceiver --receiver-permission android.permission.FOREGROUND_SERVICE",
             PKGNAME, PKGNAME);
    system(cmd);
}

void get_top_app(char *output, size_t size) {
    FILE *fp;
    char line[512];
    char result[512] = "";

    fp = popen("dumpsys activity activities 2>/dev/null | grep 'ResumedActivity' | head -n1", "r");
    if (fp == NULL) {
        snprintf(output, size, "");
        return;
    }

    while (fgets(line, sizeof(line), fp) != NULL) {
        char *last_space = strrchr(line, ' ');
        if (last_space != NULL)
            *last_space = '\0';

        char *second_last_space = strrchr(line, ' ');
        if (second_last_space != NULL) {
            snprintf(result, sizeof(result), "%s", second_last_space + 1);
            result[strcspn(result, "\n")] = 0;
        }

        break;
    }

    pclose(fp);

    if (strlen(result) > 0)
        snprintf(output, size, "-n %s", result);
    else
        snprintf(output, size, "-a android.intent.action.MAIN -c android.intent.category.HOME");
}

bool is_app_installed(const char *package_name) {
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "pm list packages %s 2>/dev/null", package_name);

    FILE *fp = popen(cmd, "r");
    if (fp == NULL) return false;

    char line[256];
    bool found = false;

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, package_name)) {
            found = true;
            break;
        }
    }

    pclose(fp);
    return found;
}

static void restart_app(char *pkg, bool keep) {
	if (!is_app_installed(pkg)) return;
    char check_app_cmd[256];
    snprintf(check_app_cmd, sizeof(check_app_cmd),
        "pidof %s >/dev/null 2>&1", pkg);
    int ret = system(check_app_cmd);
    char start_cmd[512];
    char msg[512];
    if (keep && ret != 0) {
        char topApp[512];
        get_top_app(topApp, sizeof(topApp));
        snprintf(start_cmd, sizeof(start_cmd),
            "main=$(cmd package resolve-activity --brief %s | tail -n 1); am start -n $main --activity-exclude-from-recents --ez \"background\" true --activity-clear-task; am start %s",
            pkg, topApp);
		if (!(strstr(topApp, "installer") || strstr(topApp, "install") || strstr(topApp, "appdetail") || strstr(topApp, "settings"))) {
			system(start_cmd);
			snprintf(msg, sizeof(msg), "%s restarted in background", pkg);
			write_key_value(PROP_MSG, msg);
			if (strstr(pkg, PKGNAME))
				start_battery_broadcast();
		}
    } else if (!keep && ret == 0) {
        snprintf(start_cmd, sizeof(start_cmd), "am force-stop %s", pkg);
		system(start_cmd);
		snprintf(msg, sizeof(msg), "%s killed", pkg);
		write_key_value(PROP_MSG, msg);
	}
}

static void battery_notify_loop_state(int *battShow, int *chgmode, int *countBatt, char *prevNotifStatus) {
    char prefbuf[4096] = {0};
    FILE *pf = fopen(SETTINGS_PREFS, "r");
    if (pf) {
        size_t r = fread(prefbuf, 1, sizeof(prefbuf) - 1, pf);
        prefbuf[r] = '\0';
        fclose(pf);
    }
    if (!strstr(prefbuf, "battery_notif")) return; 

    int mode = 0;
    char *pmode = strstr(prefbuf, "battery_notif_mode");
    if (pmode) {
        char tmp[8] = {0};
        for (int i = 0; i < 6 && pmode[i]; ++i)
            if (pmode[i] >= '0' && pmode[i] <= '9') { tmp[0] = pmode[i]; break; }
        mode = atoi(tmp);
    }

    char *status = read_file_power(POWER_SUPPLY_DIR "/status");
    if (!status || !status[0]) status = "Unknown";

    if (strstr(status, "Discharg")) *chgmode = 3;
    else if (strstr(status, "Charging")) *chgmode = 2;
    else if (strstr(status, "Full")) *chgmode = 5;
    else if (strstr(status, "Not Charg")) *chgmode = 4;
    else *chgmode = 0;

    int shouldShow = 0;
    if (mode == 0) {
        shouldShow = 1;
    } else if (mode == 1) {
        if (strstr(status, "Charging") || strstr(status, "Full") || strstr(status, "Not Charg")) shouldShow = 1;
    }

    if (shouldShow && *battShow == 0) {
        *battShow = 1;
        start_battery_broadcast();
    } else if (!shouldShow && *battShow == 1) {
        *battShow = 0;
        start_battery_broadcast();
    }
/*
    if (*battShow == 1) {
        restart_app(PKGNAME, true);
    }*/
}

void execute_settings_command(const char* key, const char* value) {
    char command[128];
    snprintf(command, sizeof(command), "settings put system %s %s", key, value);
    system(command);
}

int get_settings_value(const char* key, char* output, size_t output_size) {
    char command[128];
    snprintf(command, sizeof(command), "settings get system %s", key);

    FILE* fp = popen(command, "r");
    if (fp == NULL) {
        return -1;
    }

    if (fgets(output, output_size, fp) == NULL) {
        pclose(fp);
        return -1;
    }

    output[strcspn(output, "\n")] = 0;
    return pclose(fp);
}

int read_prefs_in_xml(const char *xml_path, const char *name, const char *value) {
    FILE *fp = fopen(xml_path, "r");
    if (!fp) return 0;
    char line[512];
    int result = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name) && strstr(line, value)) {
            result = 1;
            break;
        }
    }
    fclose(fp);
    return result;
}

void set_auto_brightness(const char *mode) {
    char brightness_mode[16];

    if (get_settings_value("screen_brightness_mode", brightness_mode, sizeof(brightness_mode)) == 0) {
        if (strcmp(brightness_mode, mode) != 0 && 
            read_prefs_in_xml(SETTINGS_PREFS, "auto_brightness_backup", "true")) {
            execute_settings_command("screen_brightness_mode", "1");
        }
    }
}

int check_bypass_charging(const char *prefs) {
    FILE *file = NULL;
    char line[1024];
    int result = 0;

    file = fopen(prefs, "r");
    if (file == NULL) {
        fprintf(stderr, "Error opening file %s: %s\n", prefs, strerror(errno));
        return 0;
    }

    while (fgets(line, sizeof(line), file)) {
        if (strstr(line, "bypass_charging") != NULL) {
            result = 1;
            break;
        }
    }

    if (file) {
        fclose(file);
    }

    return result;
}

void bypass_charging_set(const char *val) {
	write_file(INPUT_SUSPEND, val);
	write_key_value(PROP_BYPASS_STATUS, val);
}

void bypass_charging(const char *perfs) {
    char *input_suspend = read_file_to_buf(INPUT_SUSPEND);
    char *status = read_file_to_buf(POWER_SUPPLY_DIR "/status");
    int xml_bypass = read_prefs_in_xml(perfs, "bypass_charging", "true");

    if (!input_suspend || !status) return;

    int is_suspend_on = strstr(input_suspend, "1") != NULL;
    int is_charging = strstr(status, "Charging") != NULL || strstr(status, "Full") != NULL;

	if (xml_bypass) {
		if (!is_suspend_on && is_charging)
			bypass_charging_set("1");
    } else if (is_suspend_on)
		bypass_charging_set("0");

	free(input_suspend);
	free(status);
}

static void start_service_broadcast() {
    char cmd[1024];
    snprintf(cmd, sizeof(cmd),
             "am broadcast -a %s.PER_APP -n %s/.ui.services.PerAppReceiver --receiver-permission android.permission.FOREGROUND_SERVICE",
             PKGNAME, PKGNAME);
    // ignore return
    system(cmd);
}

static char *get_perapp_list_from_prefs() {
    char *content = read_file_trim(SETTINGS_PREFS);
    if (!content) return NULL;
    const char *needle = "perapp_list";
    char *pos = strstr(content, needle);
    if (!pos) { free(content); return NULL; }
    char *gt = strchr(pos, '>');
    if (!gt) { free(content); return NULL; }
    gt++;
    char *end = strstr(gt, "</string>");
    if (!end) { free(content); return NULL; }
    size_t len = end - gt;
    char *res = malloc(len + 1);
    memcpy(res, gt, len);
    res[len] = '\0';
    free(content);
    return res;
}

static void keep_or_kill_app(void) {
    char buffer[512];
	if (!get_perapp_list_from_prefs()) return;
    strncpy(buffer, get_perapp_list_from_prefs(), sizeof(buffer));
    buffer[sizeof(buffer) - 1] = '\0';

    char *token = strtok(buffer, " ");
    while (token != NULL) {
        char filepath[1024];
        snprintf(filepath, sizeof(filepath), "%s/%s.xml", SHARED_PREFS, token);

        if (file_exists(filepath)) {
			if (read_prefs_in_xml(filepath, "kill_app", "true"))
				restart_app(token, false);
			else if (read_prefs_in_xml(filepath, "keep_app", "true"))
				restart_app(token, true);
        }

        token = strtok(NULL, " ");
    }
}

static void setprop_topapp(const char *top_app) {
	write_key_value(PROP_TOPAPP, top_app);
}

static void setprop_perapp(const char *app) {
	write_key_value(PROP_PERAPP, app);
}

static char *get_top_app_from_dumpsys() {
    char *output = run_command_capture("dumpsys window | grep 'mFocusedApp' 2>/dev/null");
    if (!output) return NULL;

    char *last_line = NULL;
    char *line = strtok(output, "\n");
    while (line) {
        last_line = line;
        line = strtok(NULL, "\n");
    }

    if (!last_line) {
        free(output);
        return NULL;
    }

    char *slash = strchr(last_line, '/');
    if (!slash) {
        free(output);
        return NULL;
    }

    char *p = slash;
    while (p > last_line && *p != ' ' && *p != '\t') p--;
    while (*p == ' ' || *p == '\t') p++;

    char *start = p;
    size_t len = 0;
    while (start[len] && start[len] != '/' && start[len] != ' ' &&
           start[len] != '\t' && start[len] != '\n') {
        len++;
    }

    char *pkg = malloc(len + 1);
    if (!pkg) {
        free(output);
        return NULL;
    }
    memcpy(pkg, start, len);
    pkg[len] = '\0';

    free(output);
    return pkg;
}

static char *get_toplist_from_dumpsys_windows() {
    char *out = run_command_capture("dumpsys window windows | grep -i topapp 2>/dev/null");
    return out;
}

static char *get_pidof(const char *pkg) {
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "pidof %s 2>/dev/null", pkg);
    char *out = run_command_capture(cmd);
    if (!out) return NULL;
    size_t L = strlen(out);
    while (L > 0 && (out[L-1] == '\n' || out[L-1] == '\r')) { out[L-1] = '\0'; L--; }
    return out;
}

static void get_perapp_list_loop_iteration() {
    char *list_app = get_perapp_list_from_prefs();
    char *top_app = get_top_app_from_dumpsys();
    char *toplist = get_toplist_from_dumpsys_windows();
    if (top_app) {
        setprop_topapp(top_app);
    } else {
        // fallback
        top_app = strdup("unknown");
    }

    if (strlen(appidrun) > 0) {
        strncpy(appidlast, appidrun, sizeof(appidlast)-1);
    } else {
        appidlast[0] = '\0';
    }
    char *pid = get_pidof(PKGNAME);
    if (pid) {
        strncpy(appidrun, pid, sizeof(appidrun)-1);
        free(pid);
    } else {
        appidrun[0] = '\0';
    }

    if (list_app && strlen(list_app) > 0) {
        if (strlen(appidlast) > 0 && strcmp(appidlast, appidrun) != 0) {
            start_service_broadcast();
        }

        if (strstr(list_app, top_app) != NULL) {
            ongame = 1;
            if (strcmp(focused_app, top_app) != 0) {
                strncpy(focused_app, top_app, sizeof(focused_app)-1);
                setprop_perapp(top_app);
                start_service_broadcast();
            }
            if (strlen(appidrun) == 0) {
                start_service_broadcast();
            }
        } else {
            if (toplist && strstr(toplist, focused_app) != NULL) {
                ongame = 1;
                if (strlen(appidrun) == 0) {
                    start_service_broadcast();
                }
            } else {
                if (ongame == 1) {
                    ongame = 0;
                    strncpy(focused_app, "not focused", sizeof(focused_app)-1);
					setprop_perapp(top_app);
                    start_service_broadcast();
					set_auto_brightness("1");
                }
            }
        }
    }

    if (ongame == 1 && strcmp(focused_app, "not focused") != 0) {
        char perapp_prefs_path[1024];
        snprintf(perapp_prefs_path, sizeof(perapp_prefs_path), "%s/%s.xml", SHARED_PREFS, focused_app);
		int bypass_xml = read_prefs_in_xml(perapp_prefs_path, "bypass_charging", "true");
		if (check_bypass_charging(perapp_prefs_path) && bypass_xml)
			bypass_charging(perapp_prefs_path);
		else
			bypass_charging(SETTINGS_PREFS);
    } else {
        bypass_charging(SETTINGS_PREFS);
    }

    free(list_app);
    free(top_app);
    free(toplist);
}

static void pif_updater() {
    const char *pifbin = "/vendor/bin/oemports10t_PIF-updater";
    struct stat st;
    if (stat(pifbin, &st) == 0) {
        char outpath[512];
        snprintf(outpath, sizeof(outpath), "%s/pif.txt", FILES_DIR);
		
        char cmd[1024];
        snprintf(cmd, sizeof(cmd), "su -c '%s' > %s 2>&1", pifbin, outpath);
        system(cmd);
		
        char *getmsg = read_value_by_key(PROP_MSG, "");
		
        if (!getmsg)
			write_key_value(PROP_MSG, "pif finish");
        else {
            char buf[512];
            snprintf(buf, sizeof(buf), "%s-pif finish", getmsg);
            write_key_value(PROP_MSG, buf);
        }
    }
}

static void kill_service(const char *pid_str) {
    if (!pid_str || strlen(pid_str) == 0) return;

    int pid = atoi(pid_str);
    if (pid <= 0) return;

    pid_t self = getpid();

    if (pid != self) {
        if (kill(pid, SIGTERM) == 0) {
            usleep(300000);
            if (kill(pid, 0) == 0) {
                kill(pid, SIGKILL);
            }
        }
    }

    write_key_value(PROP_PID, "0");

    if (pid == self) {
        printf("dkmsvc daemon exiting (self-killed)\n");
        exit(0);
    }
}

static void clear_cmd_and_handle(const char *rd) {
    if (!rd || !rd[0]) return;
    if (strstr(rd, "battery")) {
        char cmd[512];
        snprintf(cmd, sizeof(cmd), "am broadcast -a %s.BATTERY_INFO -n %s/.ui.services.BatteryReceiver --receiver-permission android.permission.FOREGROUND_SERVICE", PKGNAME, PKGNAME);
        system(cmd);
        write_key_value(PROP_CMD, "");
    } else if (strstr(rd, "pif")) {
        pif_updater();
        write_key_value(PROP_CMD, "");
    }
}

static void helpers(bool mode) {
	if (mode) {
		printf("Usage: dkmsvc [option]\n");
		printf("Options:\n");
		printf("  service	- start daemon\n");
		printf("  stop		- stop daemon\n");
		printf("  version	- show version\n");
		printf("  hash		- show binary hash\n");
		printf("  wakelock	- generate wakelock\n");
	} else {
        printf("dkmsvc: missing command.\n");
        printf("Try 'dkmsvc' for more information.\n");
	}
}

// Lag detections
static void restart_broadcast() {
    char cmd[1024];
    snprintf(cmd, sizeof(cmd),
             "am broadcast -a %s.SVC_RESTART -n %s/.ui.services.SVCReceiver --receiver-permission android.permission.FOREGROUND_SERVICE",
             PKGNAME, PKGNAME);
    // ignore return
    system(cmd);
}

void drop_caches(void) {
    sync();
	write_file("/proc/sys/vm/drop_caches", "3");
	write_file("/proc/sys/vm/compact_memory", "1");
}

static int detect_system_lag() {
    struct timespec start, end;
    
    // Measure only fopen + fread time
    clock_gettime(CLOCK_MONOTONIC, &start);
    
    pid_t pid = getpid();
    char proc_path[64];
    snprintf(proc_path, sizeof(proc_path), "/proc/%d/stat", pid);
    
    FILE *f = fopen(proc_path, "r");
    if (!f) return 1;
    
    char buffer[256];
    size_t bytes = fread(buffer, 1, sizeof(buffer)-1, f);
    fclose(f);
    
    clock_gettime(CLOCK_MONOTONIC, &end);
    
    if (bytes == 0) return 1;
    
    long elapsed_ms = (end.tv_sec - start.tv_sec) * 1000 + 
                     (end.tv_nsec - start.tv_nsec) / 1000000;
    
    // If file operation takes > 100ms, system is lagging
    return (elapsed_ms > 100) ? 1 : 0;
}

// Check system load
static int check_system_load() {
    char *load_avg = read_file_trim("/proc/loadavg");
    if (!load_avg) return 0;
    
    float load_1min, load_5min, load_15min;
    sscanf(load_avg, "%f %f %f", &load_1min, &load_5min, &load_15min);
    free(load_avg);
    
    // Get number of CPU cores using sysconf (more reliable)
    int cores = sysconf(_SC_NPROCESSORS_CONF);
    if (cores <= 0) cores = 1;
    
    // Consider lag if 1-min load > 80% of cores
    return (load_1min > cores * 0.8) ? 1 : 0;
}

static int check_psi_pressure() {
    // Check I/O pressure
    char *io_psi = read_file_trim("/proc/pressure/io");
    if (io_psi) {
        float avg10 = 0, avg60 = 0, avg300 = 0;
        sscanf(io_psi, "some avg10=%f avg60=%f avg300=%f", &avg10, &avg60, &avg300);
        free(io_psi);
        
        if (avg10 > PSI_STALL_THRESHOLD) {
            //printf("I/O pressure detected: %.1f%% stall\n", avg10);
            return 1;
        }
    }
    
    // Check memory pressure (if available, kernel >= 4.14)
    char *mem_psi = read_file_trim("/proc/pressure/memory");
    if (mem_psi) {
        float avg10 = 0, avg60 = 0, avg300 = 0;
        sscanf(mem_psi, "some avg10=%f avg60=%f avg300=%f", &avg10, &avg60, &avg300);
        free(mem_psi);
        
        if (avg10 > PSI_STALL_THRESHOLD) {
            //printf("Memory pressure detected: %.1f%% stall\n", avg10);
            return 1;
        }
    }
    
    return 0;
}

// Check daemon CPU usage
static float get_daemon_cpu_usage() {
    static unsigned long long last_utime = 0, last_stime = 0;
    static struct timespec last_time;
    static int initialized = 0;
    
    pid_t pid = getpid();
    char stat_path[64];
    snprintf(stat_path, sizeof(stat_path), "/proc/%d/stat", pid);
    
    FILE *f = fopen(stat_path, "r");
    if (!f) return 0.0;
    
    char buffer[1024];
    if (fread(buffer, 1, sizeof(buffer)-1, f) == 0) {
        fclose(f);
        return 0.0;
    }
    fclose(f);
    
    // Parse utime and stime using sscanf (more robust than strtok)
    unsigned long long utime = 0, stime = 0;
    // Skip first 13 fields, then read utime (14th) and stime (15th)
    sscanf(buffer, "%*d %*s %*c %*d %*d %*d %*d %*d %*u %*u %*u %*u %*u %llu %llu", 
           &utime, &stime);
    
    struct timespec current_time;
    clock_gettime(CLOCK_MONOTONIC, &current_time);
    
    unsigned long long total_time = utime + stime;
    
    if (!initialized) {
        last_utime = utime;
        last_stime = stime;
        last_time = current_time;
        initialized = 1;
        return 0.0;
    }
    
    // Calculate time difference in seconds
    double time_diff = (current_time.tv_sec - last_time.tv_sec) + 
                      (current_time.tv_nsec - last_time.tv_nsec) / 1000000000.0;
    
    if (time_diff <= 0) return 0.0;
    
    unsigned long long total_diff = total_time - (last_utime + last_stime);
    long clock_tick = sysconf(_SC_CLK_TCK);
    if (clock_tick <= 0) clock_tick = 100;
    
    float cpu_usage = (total_diff * 100.0) / (time_diff * clock_tick);
    
    // Update last values
    last_utime = utime;
    last_stime = stime;
    last_time = current_time;
    
    // Cap at 100%
    if (cpu_usage > 100.0) cpu_usage = 100.0;
    
    return cpu_usage;
}

static void performance_monitor(const char *pidstr) {
    time_t now = time(NULL);

    if (now - last_restart > 3600) {
        restart_count = 0;
        last_restart = now;
    }

    if (now - last_perf_check < PERFORMANCE_MONITOR_INTERVAL) {
        return;
    }
    last_perf_check = now;

    int lag_detected = 0;

    if (detect_system_lag()) {
        lag_detected = 1;
        printf("Lag detected: Slow file operation\n");
    }

    if (check_system_load()) {
        lag_detected = 1;
        printf("Lag detected: High system load\n");
    }

    if (check_psi_pressure()) {
        lag_detected = 1;
		drop_caches();
        printf("Lag detected: System resource pressure (PSI)\n");
    }

    float cpu_usage = get_daemon_cpu_usage();
    if (cpu_usage > 50.0) {
        lag_detected = 1;
        printf("Lag detected: High CPU usage %.2f%%\n", cpu_usage);
    }

    if (lag_detected) {
        lag_detected_count++;
        printf("Lag count: %d/%d\n", lag_detected_count, LAG_THRESHOLD);

        if (lag_detected_count >= LAG_THRESHOLD) {
            if (restart_count < MAX_RESTARTS_PER_HOUR) {
                printf("Critical lag detected, restarting daemon...\n");
                write_key_value(PROP_MSG, "daemon restarting due to lag");

				restart_broadcast();
                kill_service(pidstr);
                exit(EXIT_SUCCESS);
            } else {
                printf("Max restarts per hour reached, waiting...\n");
                write_key_value(PROP_MSG, "max restarts reached, pausing");
                lag_detected_count = 0;
            }
        }
    } else {
        if (lag_detected_count > 0) {
            lag_detected_count--;
        }
    }
}

char* get_screen_state() {
    static char screen_state[MAX_BUFFER];
    screen_state[0] = '\0';

    // Try multiple methods and see which one works
    char* result = NULL;
	char panel_status_path[256];

	if (file_exists(OPLUS_SCREEN)) {
		snprintf(panel_status_path, sizeof(panel_status_path), OPLUS_SCREEN);
		char* ps = read_file_to_buf(panel_status_path);
		if (strcmp(ps, "2") == 0) 
			strcpy(screen_state, "on");
		else {
			strcpy(screen_state, "off");
		}
		free(ps);
	} else {
		execute_command("dumpsys power | grep -E 'mScreenOn|mDisplayReady|mWakefulness|Display Power'", &result);
		if (result != NULL) {
			if (strstr(result, "mScreenOn=true") || 
				strstr(result, "mDisplayReady=true") ||
				strstr(result, "Display Power=ON") ||
				strstr(result, "mWakefulness=Awake") ||
				strstr(result, "mWakefulness=Waking")) {
				strcpy(screen_state, "on");
			} else if (strstr(result, "mScreenOn=false") || 
					   strstr(result, "mDisplayReady=false") ||
					   strstr(result, "Display Power=OFF") ||
					   strstr(result, "mWakefulness=Asleep") ||
					   strstr(result, "mWakefulness=Dozing")) {
				strcpy(screen_state, "off");
			} else {
				strcpy(screen_state, "unknown");
			}
			free(result);
		} else {
			strcpy(screen_state, "unknown");
		}
	}

    return screen_state;
}

int main(int argc, char *argv[]) {
	struct stat st;

	init_config();

    char *gpid = read_value_by_key(PROP_PID, "0");

    write_key_value(PROP_VERSION, "v" VERSION);

    char binpath[512];
    snprintf(binpath, sizeof(binpath), "%s/dkmsvc", DIR_MAIN);
    char *hashsvc = compute_md5(binpath);
    if (!hashsvc || !hashsvc[0]) {
        hashsvc = "";
    }
    write_key_value("hash", hashsvc);

    if (argc > 1) {
        if (strcmp(argv[1], "version") == 0) {
            printf("%s\n", "v" VERSION);
            return 0;
        } else if (strcmp(argv[1], "hash") == 0) {
            char *h = compute_md5(binpath);
            printf("%s\n", h);
            return 0;
        } else if (strcmp(argv[1], "status") == 0) {
			if (argc > 2) {
				if (strcmp(argv[2], PROP_BYPASS_STATUS) == 0)
					printf("%s\n", read_value_by_key(PROP_BYPASS_STATUS, "0"));
				else if (strcmp(argv[2], PROP_TOPAPP) == 0)
					printf("%s\n", read_value_by_key(PROP_TOPAPP, ""));
				else if (strcmp(argv[2], PROP_PERAPP) == 0)
					printf("%s\n", read_value_by_key(PROP_PERAPP, ""));
				else if (strcmp(argv[2], PROP_MSG) == 0)
					printf("%s\n", read_value_by_key(PROP_MSG, ""));
			} else {
				if (strcmp(gpid, "0") != 0)
					printf("started\n");
				else
					printf("stopped");
			}
            return 0;
        } else if (strcmp(argv[1], "bypass") == 0) {
			bypass_charging(SETTINGS_PREFS);
			return 0;
        } else if (strcmp(argv[1], "cmd") == 0) {
			if (argc > 2) {
				write_key_value(PROP_CMD, argv[2]);
			} else {
				printf("%s\n", read_value_by_key(PROP_CMD, ""));
			}
            return 0;
        } else if (strcmp(argv[1], "wakelock") == 0) {
			int to_file = (argc > 2 && strcmp(argv[2], "file") == 0);
			generate_wakelock(to_file);
            return 0;
        } else if (strcmp(argv[1], "battery") == 0) {
            char cmd[512];
            snprintf(cmd, sizeof(cmd), "am broadcast -a %s.BATTERY_INFO -n %s/.ui.services.BatteryReceiver --receiver-permission android.permission.FOREGROUND_SERVICE", PKGNAME, PKGNAME);
            system(cmd);
            return 0;
        } else if (strcmp(argv[1], "auto_brightness") == 0) {
			if (argc > 2 && (strcmp(argv[2], "1") == 0 || strcmp(argv[2], "0") == 0)) {
				set_auto_brightness(argv[2]);
			}
            return 0;
		} else if (strcmp(argv[1], "service") == 0) {
            write_key_value(PROP_EXIT, "0");
            if (gpid && strcmp(gpid, "0") != 0) {
                int pid_running = atoi(gpid);
                if (pid_running > 0) {
                    char proc_path[64];
                    snprintf(proc_path, sizeof(proc_path), "/proc/%d", pid_running);
                    if (access(proc_path, F_OK) == 0) {
                        printf("dkmsvc daemon already running (PID %d)\n", pid_running);
                        return 0;
                    }
                }
            }

            pid_t pid = fork();
            if (pid < 0) {
                perror("fork failed");
                exit(EXIT_FAILURE);
            }
            if (pid > 0) {
                printf("dkmsvc started as daemon (PID %d)\n", pid);
                exit(EXIT_SUCCESS);
            }

            setsid();
            umask(0);
            chdir("/");

            freopen("/dev/null", "r", stdin);
            freopen("/dev/null", "w", stdout);
            freopen("/dev/null", "w", stderr);

            char pidstr[64];
            snprintf(pidstr, sizeof(pidstr), "%d", getpid());
            write_key_value(PROP_PID, pidstr);

            goto run_daemon;
        } else if (strcmp(argv[1], "stop") == 0) {
            if (gpid && strcmp(gpid, "0") != 0) {
                int pid_running = atoi(gpid);
                if (pid_running > 0) {
                    char proc_path[64];
                    snprintf(proc_path, sizeof(proc_path), "/proc/%d", pid_running);
                    if (access(proc_path, F_OK) == 0) {
                        printf("Stopping dkmsvc daemon (PID %d)...\n", pid_running);
                        kill(pid_running, SIGTERM);
                        usleep(300000);
                        if (access(proc_path, F_OK) == 0) {
                            kill(pid_running, SIGKILL);
                        }
                    } else {
                        printf("No active dkmsvc daemon found.\n");
                    }
                }
            }
            write_key_value(PROP_PID, "0");
            write_key_value(PROP_EXIT, "1");
            printf("dkmsvc stopped.\n");
            return 0;
        } else {
			helpers(false);
            return 0;
		}
    } else {
		helpers(true);
		return 0;
	}

    return 0;

run_daemon:
    /* --- Start of daemon main loop --- */
    if (stat(FILES_DIR, &st) != 0) mkdir(FILES_DIR, 0755);
    if (stat(SUSFS_DIR, &st) != 0) mkdir(SUSFS_DIR, 0755);

    int start_ds = 0;
    int check_main_dir = 0;
    int battShow = 0;
    int chgmode = 0;
    int countBatt = 0;
    int daemon = 0;
    char prevNotifStatus[128] = {0};

    /* main loop replicating sleep 1 and checks */
    while (1) {
		char pidstr[64];
		snprintf(pidstr, sizeof(pidstr), "%d", getpid());
		char *pid_prop = read_value_by_key(PROP_PID, "0");

        if (strcmp(pid_prop, pidstr) != 0) {
            printf("Detected old daemon (PID %s), killing...\n", pid_prop);
            kill_service(pid_prop);
			write_key_value(PROP_PID, pidstr);
        }

		/* exit requested? */
		char *exit_flag = read_value_by_key(PROP_EXIT, "0");
		if (exit_flag && strcmp(exit_flag, "1") == 0) {
			kill_service(pid_prop);
			break;
		}

		char *hashnow = compute_md5(binpath);
		if (hashnow && hashnow[0] && strcmp(hashsvc, hashnow) != 0) {
			kill_service(pidstr);
			break;
		}

		char* screenOn = get_screen_state();
		if (strcmp(screenOn, "on") == 0) {
			start_ds = 0;
			// Performance monitoring
			performance_monitor(pid_prop);
			/* Game Mode */
			get_perapp_list_loop_iteration();
			/* Battery Monitor */
			battery_notify_loop_state(&battShow, &chgmode, &countBatt, prevNotifStatus);
			/* Read CMD */
			char *rd = read_value_by_key(PROP_CMD, "");
			if (rd) {
				clear_cmd_and_handle(rd);
			}

			/* Keep per-app running */
			keep_or_kill_app();
		} else {
			if (start_ds == 0) {
				start_ds = 1;
				char *input_suspend = read_file_to_buf(INPUT_SUSPEND);
				if (strstr(input_suspend, "1"))
					bypass_charging_set("0");
				free(input_suspend);
			}

			/* tetap cek status baterai jika sedang charging */
			char *status = read_file_power(POWER_SUPPLY_DIR "/status");
			if (status && strstr(status, "Charging")) {
				battery_notify_loop_state(&battShow, &chgmode, &countBatt, prevNotifStatus);
			}
		}
        struct timespec ts = { .tv_sec = 1, .tv_nsec = 0 };
        nanosleep(&ts, NULL);
	}

    return 0;
}
