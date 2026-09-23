package dev.ancaria.coderpack.api;

/**
 * API 2's SacredMod, which was an interface. Here only so a fixture can be
 * compiled the way a mod written for that API was. No test packs this class;
 * they pack the mod that implements it.
 */
public interface SacredMod {

    void onLoad(Context context);
}
