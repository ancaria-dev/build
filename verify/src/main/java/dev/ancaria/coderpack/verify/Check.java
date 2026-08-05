package dev.ancaria.coderpack.verify;

import java.util.List;

/** One rule about one jar. Adds what it found and never throws. */
interface Check {

    void run(Mod mod, List<Finding> found);
}
