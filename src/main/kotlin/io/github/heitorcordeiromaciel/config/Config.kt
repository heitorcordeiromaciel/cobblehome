package io.github.heitorcordeiromaciel.config

import net.neoforged.neoforge.common.ModConfigSpec

object CobbleHomeConfig {

    val SPEC: ModConfigSpec
    val VALUES: Values

    init {
        val builder = ModConfigSpec.Builder()
        VALUES = Values(builder)
        SPEC = builder.build()
    }

    class Values(builder: ModConfigSpec.Builder) {

        val enableGlobalHome: ModConfigSpec.BooleanValue

        init {
            builder.push("global_home")

            enableGlobalHome = builder
                .comment("Enable shared Cobblemon Home across modpacks")
                .define("enableGlobalHome", true)

            builder.pop()
        }
    }
}