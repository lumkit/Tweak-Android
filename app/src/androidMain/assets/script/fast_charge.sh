#!/system/bin/sh

# 默认充电限制值（毫安）
DEFAULT_LIMIT=3000

# 读取输入参数，设置默认值
limit_value="${1:-$DEFAULT_LIMIT}"
only_taller="${2:-0}"

# 验证 limit_value 是否为正整数
if ! echo "$limit_value" | grep -qE '^[0-9]+$'; then
    echo "错误: limit_value ('$limit_value') 必须是一个正整数。"
    exit 1
fi

# 将 limit_value 转换为微安（假设系统需要微安单位）
limit="${limit_value}000"

# 查找所有 constant_charge_current_max 文件路径
paths=$(find /sys/class/power_supply/ -type f -name "constant_charge_current_max" 2>/dev/null)

# 检查是否找到任何路径
if [ -z "$paths" ]; then
    echo "错误: 未找到任何 'constant_charge_current_max' 文件。"
    exit 1
fi

# 定义更改充电限制的函数
change_limit() {
    local new_limit="$1"
    echo "更改限制值为：${limit_value}mA"

    for path in $paths; do
        # 检查文件是否可写，如果不可写则尝试更改权限
        if [ ! -w "$path" ]; then
            chmod 0664 "$path" 2>/dev/null
            if [ $? -ne 0 ]; then
                echo "警告: 无法更改权限: $path"
                continue
            fi
        fi

        if [ "$only_taller" -eq 1 ]; then
            current_limit=$(cat "$path" 2>/dev/null)
            if [ -z "$current_limit" ]; then
                echo "警告: 无法读取当前限制值: $path"
                continue
            fi

            if [ "$current_limit" -lt "$limit" ]; then
                echo "$limit" > "$path" 2>/dev/null
                if [ $? -eq 0 ]; then
                    echo "已将 $path 的限制设置为 ${limit_value}mA"
                else
                    echo "错误: 无法写入限制值到: $path"
                fi
            else
                echo "跳过: $path 的当前限制值 ($current_limit) >= 新限制值 ($limit)。"
            fi
        else
            echo "$limit" > "$path" 2>/dev/null
            if [ $? -eq 0 ]; then
                echo "已将 $path 的限制设置为 ${limit_value}mA"
            else
                echo "错误: 无法写入限制值到: $path"
            fi
        fi
    done
}

# 初始化 fast_charge（如果尚未初始化）
if [ "$(getprop vtools.fastcharge)" = "" ]; then
    if [ -x "./fast_charge_run_once.sh" ]; then
        ./fast_charge_run_once.sh
        if [ $? -eq 0 ]; then
            setprop vtools.fastcharge 1
            echo "已初始化 fast_charge_run_once.sh 并设置 vtools.fastcharge=1"
        else
            echo "错误: 运行 fast_charge_run_once.sh 失败"
            exit 1
        fi
    else
        echo "错误: fast_charge_run_once.sh 不存在或不可执行"
        exit 1
    fi
fi

# 更改充电限制
change_limit "$limit"

# 可选: 记录操作日志
# echo "$(date "+%Y-%m-%d %H:%M:%S") -> $limit_value mA" >> /cache/scene_charge.log
