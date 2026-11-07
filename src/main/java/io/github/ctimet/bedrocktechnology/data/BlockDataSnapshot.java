package io.github.ctimet.bedrocktechnology.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 机器数据快照工具。
 *
 * 原实现依赖旧版 BlockStorage 的 {@code getBlockInfoAsJson(Location)} 与
 * {@code setBlockInfo(Location, String, boolean)}，这两个方法在 Slimefun4 2025.1
 * 中已被移除。本类改用现行 API 等价实现：
 *  - 快照：{@link BlockStorage#getLocationInfo(Location)} 拿到全部键值对，用 Gson 序列化；
 *  - 恢复：反序列化后逐键调用 {@link BlockStorage#addBlockInfo(Location, String, String)} 写回。
 *
 * 已知差异（相对旧版 setBlockInfo 的全量恢复）：本实现只恢复方块数据键值对，
 * 不做旧版内部那一套「按 id 重新注册机器 + 恢复库存」的联动。若机器数据
 * 中带有 Slimefun 自身管理的关键键（如 id），addBlockInfo 同样会写回，机器
 * 可被 Slimefun 重新识别。
 */
public final class BlockDataSnapshot {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

    private BlockDataSnapshot() {
    }

    /**
     * 对一个方块当前的 Slimefun 数据做 JSON 快照。
     *
     * @return JSON 字符串；方块无任何数据时返回 "{}"
     */
    @Nonnull
    public static String snapshot(@Nonnull Location location) {
        Config info = BlockStorage.getLocationInfo(location);
        Map<String, String> data = new LinkedHashMap<>();
        if (info != null) {
            for (String key : info.getKeys()) {
                Object value = info.getValue(key);
                data.put(key, value == null ? "" : value.toString());
            }
        }
        return GSON.toJson(data);
    }

    /**
     * 把一份快照写回方块。
     *
     * @param json 之前由 {@link #snapshot(Location)} 产生的 JSON
     * @return 是否写回了任何数据
     */
    public static boolean restore(@Nonnull Location location, @Nullable String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        Map<String, String> data = GSON.fromJson(json, MAP_TYPE);
        if (data == null || data.isEmpty()) {
            return false;
        }
        data.forEach((key, value) -> BlockStorage.addBlockInfo(location, key, value));
        return true;
    }
}
