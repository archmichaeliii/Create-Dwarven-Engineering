package com.archmichaeliii.dwarvenengineering;

import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;

/**
 * Thin convenience wrapper around Create/Catnip's {@link Lang} builder, scoped to this mod's id —
 * mirrors Petrochem's PetrochemLang. Used for goggle tooltips.
 */
public class DwarvenLang extends Lang {

    public static LangBuilder builder() {
        return new LangBuilder(DwarvenEngineering.MODID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }
}
