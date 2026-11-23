package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.emberbocor.villagertraderotation.rotation.MapCache;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;

@Mixin(ExplorationMapFunction.class)
public abstract class ExplorationMapFunctionMixin {
    @Shadow
    @Final
    private HolderSet<Structure> destination;

    @Shadow
    @Final
    private Holder<MapDecorationType> mapDecoration;

    @Shadow
    @Final
    private byte zoom;

    @Shadow
    @Final
    private int searchRadius;

    @Shadow
    @Final
    private boolean skipKnownStructures;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void villagertraderotation$deferMap(ItemStack itemStack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack placeholder = MapCache.placeholder(itemStack, context, destination, mapDecoration, zoom, searchRadius, skipKnownStructures);
        if (placeholder != null) {
            cir.setReturnValue(placeholder);
        }
    }
}
