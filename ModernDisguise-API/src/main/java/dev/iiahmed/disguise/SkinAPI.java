package dev.iiahmed.disguise;

import dev.iiahmed.disguise.util.DisguiseUtil;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture; // [추가]
import java.util.function.Function;

/**
 * The {@code SkinAPI} class provides a mechanism to fetch Minecraft player skins using different APIs.
 * It allows fetching skins based on a generic context value, such as a UUID.
 *
 * <p>APIs included by default:</p>
 * <ul>
 * <li>Mojang API</li>
 * <li>MineTools API</li>
 * <li>MineSkin API</li>
 * </ul>
 *
 * @param <V> the type of the value used for context, typically {@code UUID}
 */
@SuppressWarnings("unused")
public class SkinAPI<V> {

    // [변경] 반환 타입이 CompletableFuture<Skin>으로 변경되었습니다.
    private final Function<Context<V>, CompletableFuture<Skin>> provider;

    /**
     * Constructs a new {@code SkinAPI} with the specified provider function.
     *
     * @param provider the function that provides a {@code Skin} given a {@code Context}
     */
    // [변경] 생성자 파라미터 타입이 비동기 함수로 변경되었습니다.
    public SkinAPI(final Function<Context<V>, CompletableFuture<Skin>> provider) {
        this.provider = provider;
    }

    /**
     * Fetches a {@code Skin} using the specified value asynchronously.
     *
     * @param value the value used to identify the context, such as a player's {@code UUID}
     * @return a {@code CompletableFuture} that will complete with the fetched {@code Skin}
     */
    // [변경] 반환 타입이 CompletableFuture<Skin>으로 변경되었습니다.
    public CompletableFuture<Skin> of(@NotNull final V value) {
        return provider.apply(() -> value);
    }

    /**
     * Fetches a {@code Skin} using the specified {@code Context} asynchronously.
     *
     * @param context the context used to provide a value for fetching the skin
     * @return a {@code CompletableFuture} that will complete with the fetched {@code Skin}
     */
    // [변경] 반환 타입이 CompletableFuture<Skin>으로 변경되었습니다.
    public CompletableFuture<Skin> of(@NotNull final Context<V> context) {
        return provider.apply(context);
    }

    /**
     * SkinAPI instance for Mojang's skin service.
     * This API fetches skins from Mojang's official session server.
     */
    // [변경] DisguiseUtil.getJSONObjectAsync를 호출하고 .thenApply를 통해 결과를 비동기적으로 변환합니다.
    public static final SkinAPI<UUID> MOJANG = new SkinAPI<>(context -> {
        final String id = context.value().toString().replace("-", "");
        final String url = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false".replace("%uuid%", id);
        return DisguiseUtil.getJSONObjectAsync(url)
                .thenApply(SkinAPI::extractSkinFromJSON);
    });

    /**
     * SkinAPI instance for MineTools skin service.
     * This API fetches skins from the MineTools API, a third-party service.
     */
    // [변경] 비동기 호출 및 결과 변환
    public static final SkinAPI<UUID> MINETOOLS = new SkinAPI<>(context -> {
        final String id = context.value().toString().replace("-", "");
        final String url = "https://api.minetools.eu/profile/%uuid%".replace("%uuid%", id);
        return DisguiseUtil.getJSONObjectAsync(url)
                .thenApply(object -> extractSkinFromJSON((JSONObject) object.get("raw")));
    });

    /**
     * SkinAPI instance for MineSkin skin service.
     * This API fetches skins from the MineSkin API, a third-party service.
     */
    // [변경] 비동기 호출 및 결과 변환
    public static final SkinAPI<UUID> MINESKIN = new SkinAPI<>(context -> {
        final String id = context.value().toString();
        final String url = "https://api.mineskin.org/get/uuid/%uuid%".replace("%uuid%", id);
        return DisguiseUtil.getJSONObjectAsync(url)
                .thenApply(object -> {
                    final JSONObject dataObject = (JSONObject) object.get("data");
                    final JSONObject texturesObject = (JSONObject) dataObject.get("texture");
                    return new Skin((String) texturesObject.get("value"), (String) texturesObject.get("signature"));
                });
    });

    /**
     * Extracts a {@code Skin} object from a JSON representation.
     *
     * @param object the JSON object containing the skin data
     * @return the extracted {@code Skin}
     */
    private static Skin extractSkinFromJSON(final JSONObject object) {
        if (object == null) return new Skin(null, null); // null 체크 추가
        String texture = "", signature = "";
        final JSONArray array = (JSONArray) object.get("properties");
        for (final Object o : array) {
            final JSONObject jsonObject = (JSONObject) o;
            if (jsonObject == null) continue;

            texture = (String) jsonObject.get("value");
            signature = (String) jsonObject.get("signature");
        }
        return new Skin(texture, signature);
    }

    /**
     * Represents a context used for providing a value to the {@code SkinAPI}.
     * This interface allows the {@code SkinAPI} to extract the necessary value for fetching skins.
     *
     * @param <V> the type of the context value
     */
    public interface Context<V> {

        /**
         * Returns the value associated with this context.
         *
         * @return the value of type {@code V}
         */
        V value();
    }
}
