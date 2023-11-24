package me.muxteam.base;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This helper class stores all the block to mat ids
 *
 */
public final class MuxBaseBlockRegistry {
    public final Set<String> DEFAULT_BLOCKS = new HashSet<>(Arrays.asList("1:0", "2:0", "3:0"));
    public final Set<Material> NOT_INTERACT_ABLE = new HashSet<>(Arrays.asList(Material.FENCE_GATE,
            Material.ACACIA_FENCE_GATE,
            Material.BIRCH_FENCE_GATE,
            Material.DARK_OAK_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE,
            Material.SPRUCE_FENCE_GATE,
            Material.TRAP_DOOR,
            Material.CAULDRON,
            Material.IRON_TRAPDOOR,
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.DRAGON_EGG,
            Material.HOPPER,
            Material.SAPLING,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM,
            Material.DISPENSER,
            Material.DROPPER,
            Material.FURNACE,
            Material.ANVIL,
            Material.WOOD_DOOR,
            Material.WOODEN_DOOR,
            Material.DARK_OAK_DOOR,
            Material.SPRUCE_DOOR,
            Material.JUNGLE_DOOR,
            Material.BIRCH_DOOR,
            Material.ACACIA_DOOR,
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.WOOD_BUTTON,
            Material.REDSTONE_COMPARATOR,
            Material.REDSTONE_COMPARATOR_OFF,
            Material.REDSTONE_COMPARATOR_ON,
            Material.STONE_PLATE,
            Material.WOOD_PLATE,
            Material.GOLD_PLATE,
            Material.IRON_PLATE,
            Material.BED_BLOCK,
            Material.BREWING_STAND,
            Material.CAKE_BLOCK,
            Material.NETHER_WARTS,
            Material.POTATO,
            Material.WHEAT,
            Material.DIODE_BLOCK_ON,
            Material.DIODE_BLOCK_OFF,
            Material.DIODE,
            Material.CARROT,
            Material.CROPS,
            Material.MINECART,
            Material.ITEM_FRAME,
            Material.PAINTING,
            Material.FURNACE,
            Material.BURNING_FURNACE));

    public final List<String> HALF_BLOCKS = Arrays.asList(
            "324:", // Wood Door
            "330:", // Iron Door
            "431:", // Dark Oak Door Item
            "430:", // Acacia Door Item
            "427:", // Spruce Door Item
            "429:", // Jungle Door Item
            "428:", // Birch Door Item
            "355:" // Bed
    );

    public final Set<Integer> BLOCK_IDS_TO_SIMPLE_REPLACE = new HashSet<>(Arrays.asList(
            158, // Dropper
            131, // Tripwire Hook
            53,
            67,
            86,
            91,
            108,
            109,
            114,
            128,
            134,
            135,
            136,
            156,
            163,
            164,
            170,
            180,
            182,
            117,
            380,
            84,
            27,
            28,
            66,
            157,
            69,
            23,
            33,
            29,
            77,
            143,
            107,
            151,
            167,
            183,
            184,
            185,
            186,
            187,
            147,
            148,
            54,
            50,
            54,
            65,
            146,
            120,
            130,
            106,
            61,
            62,
            78,
            76,
            118,
            73,
            287,
            55,
            132,
            99, // Brown Mushroom Block
            100, // Red Mushroom Block
            154 // Hopper
    ));

    public final Map<Integer, String> BLOCK_IDS_TO_REPLACE = new HashMap<>();

    public final Map<String, String> BLOCK_IDS_TO_EXACT_REPLACE = new HashMap<>();

    public MuxBaseBlockRegistry() {
        final Map<Material, Material> matsToReplace = new EnumMap<Material, Material>(Material.class) {{
            put(Material.SUGAR_CANE_BLOCK, Material.SUGAR_CANE);
            put(Material.SIGN_POST, Material.SIGN);
            put(Material.WALL_SIGN, Material.SIGN);
            put(Material.BREWING_STAND, Material.BREWING_STAND_ITEM);
            put(Material.CAKE_BLOCK, Material.CAKE);
            put(Material.BURNING_FURNACE, Material.FURNACE);
            put(Material.BED_BLOCK, Material.BED);
            put(Material.ACACIA_DOOR, Material.ACACIA_DOOR_ITEM);
            put(Material.DARK_OAK_DOOR, Material.DARK_OAK_DOOR_ITEM);
            put(Material.JUNGLE_DOOR, Material.JUNGLE_DOOR_ITEM);
            put(Material.SPRUCE_DOOR, Material.SPRUCE_DOOR_ITEM);
            put(Material.WOODEN_DOOR, Material.WOOD_DOOR);
            put(Material.BIRCH_DOOR, Material.BIRCH_DOOR_ITEM);
            put(Material.IRON_DOOR_BLOCK, Material.IRON_DOOR);
            put(Material.FLOWER_POT, Material.FLOWER_POT_ITEM);
            put(Material.CAULDRON, Material.CAULDRON_ITEM);
            put(Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON);
            put(Material.REDSTONE_LAMP_ON, Material.REDSTONE_LAMP_OFF);
            put(Material.GLOWING_REDSTONE_ORE, Material.REDSTONE_ORE);
            put(Material.DAYLIGHT_DETECTOR_INVERTED, Material.DAYLIGHT_DETECTOR);
            put(Material.REDSTONE_WIRE, Material.REDSTONE);
            put(Material.TRIPWIRE, Material.STRING);
            put(Material.DIODE_BLOCK_OFF, Material.DIODE);
            put(Material.DIODE_BLOCK_ON, Material.DIODE);
            put(Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR);
            put(Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_COMPARATOR);
        }};
        for (final Map.Entry<Material, Material> entry : matsToReplace.entrySet()) {
            BLOCK_IDS_TO_REPLACE.put(entry.getKey().getId(), entry.getValue().getId() + ":0");
        }
        matsToReplace.clear();
        BLOCK_IDS_TO_EXACT_REPLACE.put("17:4", "17:0");
        BLOCK_IDS_TO_EXACT_REPLACE.put("17:8", "17:0");

        BLOCK_IDS_TO_EXACT_REPLACE.put("17:9", "17:1");
        BLOCK_IDS_TO_EXACT_REPLACE.put("17:5", "17:1");

        BLOCK_IDS_TO_EXACT_REPLACE.put("17:10", "17:2");
        BLOCK_IDS_TO_EXACT_REPLACE.put("17:6", "17:2");

        BLOCK_IDS_TO_EXACT_REPLACE.put("17:11", "17:3");
        BLOCK_IDS_TO_EXACT_REPLACE.put("17:7", "17:3");

        BLOCK_IDS_TO_EXACT_REPLACE.put("162:8", "162:0");
        BLOCK_IDS_TO_EXACT_REPLACE.put("162:4", "162:0");

        BLOCK_IDS_TO_EXACT_REPLACE.put("162:9", "162:1");
        BLOCK_IDS_TO_EXACT_REPLACE.put("162:5", "162:1");
        BLOCK_IDS_TO_EXACT_REPLACE.put("97:0", "97:6");

        BLOCK_IDS_TO_EXACT_REPLACE.put("175:10", "31:2");
        BLOCK_IDS_TO_EXACT_REPLACE.put("175:3", "31:1");
        BLOCK_IDS_TO_EXACT_REPLACE.put("175:2", "31:1");

        final int[] array = {4, 8, 12};
        final int bannerId = Material.BANNER.getId();
        final int standingBannerId = Material.STANDING_BANNER.getId();
        final int wallBannerId = Material.WALL_BANNER.getId();
        for (int i = 0; i < 15; i++) {
            BLOCK_IDS_TO_EXACT_REPLACE.put(standingBannerId + ":" + i, bannerId + ":" + i);
            BLOCK_IDS_TO_EXACT_REPLACE.put(wallBannerId + ":" + i, bannerId + ":" + i);
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                BLOCK_IDS_TO_EXACT_REPLACE.put("18:" + array[j], "18:" + i);
                array[j] = array[j] + 1;
            }
        }
        final int[] array2 = {4, 8, 12};
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1; j++) {
                BLOCK_IDS_TO_EXACT_REPLACE.put("161:" + array2[j], "161:" + i);
                array2[j] = array2[j] + 1;
            }
        }
    }
}