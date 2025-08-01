package dev.iiahmed.disguise;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;
import java.util.concurrent.CompletableFuture; // [추가]
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Disguise {

    private final String name;
    private final Skin skin;
    private final Entity entity;

    private Disguise(final String name, final Skin skin, final Entity entity) {
        this.name = name;
        this.skin = skin;
        this.entity = entity;
    }

    /**
     * Returns a new instance of the Disguise.Builder class
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a {@link Boolean} that indicates whether the disguise is empty or not
     */
    public boolean isEmpty() {
        return !hasName() && !hasSkin() && !hasEntity();
    }

    /**
     * @return a {@link Boolean} that indicates whether the disguise will change the player's entity
     */
    public boolean hasEntity() {
        return entity != null && entity.isValid();
    }

    /**
     * @return a {@link Boolean} that indicates whether the disguise will change the player's name
     */
    public boolean hasName() {
        return name != null && !name.isEmpty();
    }

    /**
     * @return a {@link Boolean} that indicates whether the disguise will change the player's skin
     */
    public boolean hasSkin() {
        return skin != null && skin.isValid();
    }

    /**
     * @return the name that the disguised player's name going to be changed for
     */
    public String getName() {
        return name;
    }

    /**
     * @return the textures that the disguised player's skin going to be changed for
     */
    public String getTextures() {
        if (skin == null) {
            return null;
        }
        return skin.getTextures();
    }

    /**
     * @return the signature that the disguised player's skin going to be changed for
     */
    public String getSignature() {
        if (skin == null) {
            return null;
        }
        return skin.getSignature();
    }

    /**
     * @return the entity that the disguised player's entity going to be changed for
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * The builder class for the disguise system
     */
    public static class Builder {

        private String name;
        private Entity entity;
        // [변경] Skin 객체 대신 CompletableFuture<Skin>을 저장합니다.
        // 스킨이 설정되지 않은 경우를 대비해 null을 가진 완료된 Future로 초기화합니다.
        private CompletableFuture<Skin> skinFuture = CompletableFuture.completedFuture(null);

        /* we don't allow constructors from outside */
        private Builder() {
        }

        /**
         * This method sets the new name of the nicked player
         *
         * @param name the replacement of the actual player name
         * @return the disguise builder
         */
        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * @param uuid this is the UUID of the needed player's skin
         * @return the disguise builder
         */
        public Builder setSkin(final UUID uuid) {
            return setSkin(SkinAPI.MOJANG, uuid);
        }

        /**
         * @deprecated now that values are generic it's easier for your IDE to use the SkinAPI provided first
         * @see Disguise.Builder#setSkin(SkinAPI, Object)
         */
        @Deprecated
        @ApiStatus.ScheduledForRemoval
        public <V> Builder setSkin(final V value, final SkinAPI<V> api) {
            // [변경] SkinAPI가 CompletableFuture를 반환하므로, 이를 skinFuture에 저장합니다.
            this.skinFuture = api.of(value);
            return this;
        }

        /**
         * @param value this is the value required by the skin api
         * @param api   determines the SkinAPI type
         * @return the disguise builder
         */
        public <V> Builder setSkin(final SkinAPI<V> api, final V value) {
            // [변경] SkinAPI가 CompletableFuture를 반환하므로, 이를 skinFuture에 저장합니다.
            this.skinFuture = api.of(value);
            return this;
        }

        /**
         * @param context this is the context required by the skin api
         * @param api     determines the SkinAPI type
         * @return the disguise builder
         */
        public <V> Builder setSkin(final SkinAPI<V> api, final SkinAPI.Context<V> context) {
            // [변경] SkinAPI가 CompletableFuture를 반환하므로, 이를 skinFuture에 저장합니다.
            this.skinFuture = api.of(context);
            return this;
        }

        /**
         * Sets the skin based on a texture and a signature
         *
         * @return the disguise builder
         */
        public Builder setSkin(final String textures, final String signature) {
            return setSkin(new Skin(textures, signature));
        }

        /**
         * Sets the skin based on a {@link Skin}
         *
         * @return the disguise builder
         */
        public Builder setSkin(final Skin skin) {
            // [변경] 이미 생성된 Skin 객체는 완료된 Future로 감싸서 저장합니다.
            this.skinFuture = CompletableFuture.completedFuture(skin);
            return this;
        }

        /**
         * @deprecated There is now a custom entity system
         * @see #setEntity(Function)
         */
        public Builder setEntityType(final EntityType type) {
            this.entity = new Entity.Builder().setType(type).build();
            return this;
        }

        /**
         * @param entity the entity the player should look like
         * @return the disguise builder
         */
        public Builder setEntity(final Entity entity) {
            this.entity = entity;
            return this;
        }

        /**
         * @param builder the entity builder the player should look like
         * @return the disguise builder
         */
        public Builder setEntity(final Function<Entity.Builder, Entity.Builder> builder) {
            this.entity = builder.apply(new Entity.Builder()).build();
            return this;
        }

        /**
         * [변경] 메소드 이름이 buildAsync로 변경되고, 반환 타입이 CompletableFuture<Disguise>로 변경되었습니다.
         * 스킨 정보를 비동기적으로 가져온 후 Disguise 객체를 생성합니다.
         *
         * @return a new instance of {@link Disguise} with the collected info, wrapped in a CompletableFuture
         */
        public CompletableFuture<Disguise> build() {
            // skinFuture가 완료되면(스킨을 가져오면), thenApply 블록이 실행됩니다.
            return skinFuture.thenApply(resolvedSkin -> new Disguise(name, resolvedSkin, entity));
        }

    }

}
