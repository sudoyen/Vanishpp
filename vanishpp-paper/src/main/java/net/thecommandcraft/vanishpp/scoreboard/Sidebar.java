package net.thecommandcraft.vanishpp.scoreboard;

import net.kyori.adventure.text.Component;

import java.util.List;

interface Sidebar {

    void render(Component title, List<Component> lines);

    void close();
}
