package org.surez.surezs_quest.api.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record NPC(
    ResourceLocation id,
    String name,
    ResourceLocation avatar
) {
    public static final Codec<NPC> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(NPC::id),
            Codec.STRING.fieldOf("name").validate(name ->
                name.isEmpty()
                    ? com.mojang.serialization.DataResult.error(() -> "NPC name must not be empty")
                    : com.mojang.serialization.DataResult.success(name)
            ).forGetter(NPC::name),
            ResourceLocation.CODEC.optionalFieldOf("avatar", ResourceLocation.parse("surezs_quest:avatars/default"))
                .forGetter(NPC::avatar)
        ).apply(instance, NPC::new)
    );
}
