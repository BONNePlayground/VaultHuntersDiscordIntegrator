//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.config;


import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;


/**
 * Configuration for loading mixins only if targeted mod is present.
 */
public abstract class MixinConfigPlugin implements IMixinConfigPlugin
{
    @Override
    public void onLoad(String mixinPackage)
    {
    }


    @Override
    public String getRefMapperConfig()
    {
        return null;
    }


    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {
    }


    @Override
    public List<String> getMixins()
    {
        return null;
    }


    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }


    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }
}
