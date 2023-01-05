using generate_loot_tables.Items;

namespace generate_loot_tables
{
    internal class Levels
    {
        // TODO add damage and enchantment to armor and tools

        static Level level1 = new()
        {
            Name = "level1.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 1),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(),
                        new EntrySand()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 1),
                    Entries = new List<Entry>
                    {
                        new EntryShield(),
                        new EntryArrows() { Weight = 5}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntrySaddle(),
                        new EntryNameTag(),
                        new EntryExplorerMap(),
                        new EntryXPBottles()
                    }
                },
                new PoolFood(1, 5),
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryLeather(),
                        new EntryEmerald(),
                        new EntryCoal(),
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                }
            }
        };

        static Level level2 = new()
        {
            Name = "level2.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 1),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(),
                        new EntrySand(),
                        new EntryClayBalls(),
                        new EntryGlowstoneDust()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 1),
                    Entries = new List<Entry>
                    {
                        new EntryShield(),
                        new EntryArrows() { Weight = 3},
                        new EntryIronHelmet(),
                        new EntryIronChestplate(),
                        new EntryIronLeggings(),
                        new EntryIronBoots(),
                        new EntryIronAxe(),
                        new EntryIronPickaxe(),
                        new EntryIronShovel(),
                        new EntryIronSword()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntrySaddle(),
                        new EntryNameTag(),
                        new EntryExplorerMap(),
                        new EntryXPBottles()
                    }
                },
                new PoolFood(1, 5),
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryLeather(),
                        new EntryEmerald(),
                        new EntryCoal(),
                        new EntryIronIngots()
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(0, 3)
            }
        };

        static Level level3 = new()
        {
            Name = "level3.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs() { Weight = 2},
                        new EntrySand() { Weight = 2},
                        new EntryClayBalls() { Weight = 2 },
                        new EntryGlowstoneDust() { Weight = 2 },
                        new EntryLapisLazuli() { Weight = 2 },
                        new EntryQuartz() { Weight = 2 },
                        new EntryLantern() { Weight = 2 },
                        new EntryVine() { Weight = 2},
                        new EntryBlueIce()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(),
                        new EntryIronHelmetEnchanted(),
                        new EntryIronChestplateEnchanted(),
                        new EntryIronLeggingsEnchanted(),
                        new EntryIronBootsEnchanted(),
                        new EntryIronAxeEnchanted(),
                        new EntryIronPickaxeEnchanted(),
                        new EntryIronShovelEnchanted(),
                        new EntryIronSwordEnchanted()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntrySaddle(),
                        new EntryNameTag(),
                        new EntryExplorerMap(),
                        new EntryXPBottles(),
                        new EntryEnchantedBooks()
                    }
                },
                new PoolFood(1, 5),
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryLeather(),
                        new EntryEmerald(),
                        new EntryCoal(),
                        new EntryIronIngots(),
                        new EntryGoldIngots()
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(0, 3)
            }
        };

        static Level level4 = new()
        {
            Name = "level4.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs() { Weight = 2},
                        new EntrySand() { Weight = 2},
                        new EntryClayBalls() { Weight = 2 },
                        new EntryGlowstoneDust() { Weight = 2 },
                        new EntryLapisLazuli() { Weight = 2 },
                        new EntryQuartz() { Weight = 2 },
                        new EntryLantern() { Weight = 2 },
                        new EntryVine() { Weight = 2},
                        new EntryBlueIce(),
                        new EntryObsidian() { Weight = 2}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 2),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(),
                        new EntryDiamondHelmet(),
                        new EntryDiamondChestplate(),
                        new EntryDiamondLeggings(),
                        new EntryDiamondBoots(),
                        new EntryDiamondAxe(),
                        new EntryDiamondPickaxe(),
                        new EntryDiamondShovel(),
                        new EntryDiamondSword()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntrySaddle(),
                        new EntryNameTag(),
                        new EntryExplorerMap(),
                        new EntryXPBottles(2) { Weight = 2 },
                        new EntryEnchantedBooks() { Weight = 2 },
                        new EntryEnderPearl(),
                        new EntryBlazeRod()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryLeather(),
                        new EntryEmerald(),
                        new EntryIronIngots(),
                        new EntryGoldIngots()
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(0, 3)
            }
        };

        static Level level5 = new()
        {
            Name = "level5.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs() { Weight = 2},
                        new EntrySand() { Weight = 2},
                        new EntryClayBalls() { Weight = 2 },
                        new EntryGlowstoneDust() { Weight = 2 },
                        new EntryLapisLazuli() { Weight = 2 },
                        new EntryQuartz() { Weight = 2 },
                        new EntryLantern() { Weight = 2 },
                        new EntryVine() { Weight = 2},
                        new EntryBlueIce(),
                        new EntryObsidian() { Weight = 2}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 2),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(),
                        new EntryDiamondHelmetEnchanted(),
                        new EntryDiamondChestplateEnchanted(),
                        new EntryDiamondLeggingsEnchanted(),
                        new EntryDiamondBootsEnchanted(),
                        new EntryDiamondAxeEnchanted(),
                        new EntryDiamondPickaxeEnchanted(),
                        new EntryDiamondShovelEnchanted(),
                        new EntryDiamondSwordEnchanted()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntrySaddle(),
                        new EntryNameTag(),
                        new EntryExplorerMap(),
                        new EntryXPBottles(2) { Weight = 2 },
                        new EntryEnchantedBooks() { Weight = 3 },
                        new EntryEnderPearl(),
                        new EntryBlazeRod()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryEmerald(),
                        new EntryIronIngots(),
                        new EntryGoldIngots(),
                        new EntryDiamonds()
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull(),
                        new EntryWitherSkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(1, 3)
            }
        };

        static Level level6 = new()
        {
            Name = "level6.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(2) { Weight = 2},
                        new EntrySand(2) { Weight = 2},
                        new EntryClayBalls(2) { Weight = 2 },
                        new EntryGlowstoneDust(2) { Weight = 2 },
                        new EntryLapisLazuli(2) { Weight = 2 },
                        new EntryQuartz(2) { Weight = 2 },
                        new EntryLantern(2) { Weight = 2 },
                        new EntryVine() { Weight = 2},
                        new EntryBlueIce(2),
                        new EntryObsidian(2) { Weight = 2}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 3),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(),
                        new EntryDiamondHelmetEnchanted(),
                        new EntryDiamondChestplateEnchanted(),
                        new EntryDiamondLeggingsEnchanted(),
                        new EntryDiamondBootsEnchanted(),
                        new EntryDiamondAxeEnchanted(),
                        new EntryDiamondPickaxeEnchanted(),
                        new EntryDiamondShovelEnchanted(),
                        new EntryDiamondSwordEnchanted()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntrySaddle(),
                        new EntryNameTag(),
                        new EntryExplorerMap(),
                        new EntryXPBottles(2) { Weight = 2 },
                        new EntryEnchantedBooks() { Weight = 3 },
                        new EntryEnderPearl(),
                        new EntryBlazeRod()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 1),
                    Entries = new List<Entry>
                    {
                        new EntryEmerald(),
                        new EntryIronIngots(),
                        new EntryDiamonds(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull(),
                        new EntryWitherSkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(1, 3)
            }
        };

        static Level level7 = new()
        {
            Name = "level7.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(2) { Weight = 2},
                        new EntrySand(2) { Weight = 2},
                        new EntryClayBalls(2) { Weight = 2 },
                        new EntryQuartz(2) { Weight = 2 },
                        new EntryLantern(2) { Weight = 2 },
                        new EntryBlueIce(2),
                        new EntryObsidian(2) { Weight = 3}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 2),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(2),
                        new EntryDiamondHelmetEnchanted(2),
                        new EntryDiamondChestplateEnchanted(2),
                        new EntryDiamondLeggingsEnchanted(2),
                        new EntryDiamondBootsEnchanted(2),
                        new EntryDiamondAxeEnchanted(2),
                        new EntryDiamondPickaxeEnchanted(2),
                        new EntryDiamondShovelEnchanted(2),
                        new EntryDiamondSwordEnchanted(2),
                        new EntryTotemOfUndying()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntryNameTag(),
                        new EntryXPBottles(2) { Weight = 2 },
                        new EntryEnchantedBooks() { Weight = 3 },
                        new EntryEnderPearl(),
                        new EntryShulkerShell()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 1),
                    Entries = new List<Entry>
                    {
                        new EntryEmerald(2),
                        new EntryIronIngots(2),
                        new EntryDiamonds(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull(),
                        new EntryWitherSkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(1, 3)
            }
        };

        static Level level8 = new()
        {
            Name = "level8.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(3),
                        new EntryClayBalls(2),
                        new EntryObsidian(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 3),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(2),
                        new EntryDiamondHelmetEnchanted(2),
                        new EntryDiamondChestplateEnchanted(2),
                        new EntryDiamondLeggingsEnchanted(2),
                        new EntryDiamondBootsEnchanted(2),
                        new EntryDiamondAxeEnchanted(2),
                        new EntryDiamondPickaxeEnchanted(2),
                        new EntryDiamondShovelEnchanted(2),
                        new EntryDiamondSwordEnchanted(2),
                        new EntryTotemOfUndying() { Weight = 2}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(0, 3),
                    Entries = new List<Entry>
                    {
                        new EntryXPBottles(2) { Weight = 2 },
                        new EntryEnchantedBooks() { Weight = 5 },
                        new EntryEnderPearl(),
                        new EntryShulkerShell()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 2),
                    Entries = new List<Entry>
                    {
                        new EntryEmerald(2),
                        new EntryIronIngots(2),
                        new EntryDiamonds(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull(),
                        new EntryWitherSkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(1, 3)
            }
        };

        static Level level9 = new()
        {
            Name = "level9.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(3),
                        new EntryClayBalls(2),
                        new EntryObsidian(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 3),
                    Entries = new List<Entry>
                    {
                        new EntryArrows(2),
                        new EntryNetheriteHelmetEnchanted(),
                        new EntryNetheriteChestplateEnchanted(),
                        new EntryNetheriteLeggingsEnchanted(),
                        new EntryNetheriteBootsEnchanted(),
                        new EntryNetheriteAxeEnchanted(),
                        new EntryNetheritePickaxeEnchanted(),
                        new EntryNetheriteShovelEnchanted(),
                        new EntryNetheriteSwordEnchanted(),
                        new EntryTotemOfUndying() { Weight = 2}
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 3),
                    Entries = new List<Entry>
                    {
                        new EntryXPBottles(2),
                        new EntryEnchantedBooks(),
                        new EntryShulkerShell()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 2),
                    Entries = new List<Entry>
                    {
                        new EntryEmerald(2),
                        new EntryIronIngots(2),
                        new EntryDiamonds(2),
                        new EntryNetheriteScraps()
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryZombieHead(),
                        new EntrySkeletonSkull(),
                        new EntryWitherSkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(1, 3)
            }
        };

        static Level level10 = new()
        {
            Name = "level10.json",
            Pools = new List<Pool>
            {
                new Pool
                {
                    Rolls = new RangeRandom(0, 2),
                    Entries = new List<Entry>
                    {
                        new EntryWoodLogs(3),
                        new EntryClayBalls(2),
                        new EntryObsidian(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(2, 4),
                    Entries = new List<Entry>
                    {
                        new EntryNetheriteHelmetEnchanted(2),
                        new EntryNetheriteChestplateEnchanted(2),
                        new EntryNetheriteLeggingsEnchanted(2),
                        new EntryNetheriteBootsEnchanted(2),
                        new EntryNetheritePickaxeEnchanted(2),
                        new EntryNetheriteSwordEnchanted(2),
                        new EntryTotemOfUndying() { Weight = 2},
                        new EntryElytra(),
                        new EntryFireworkRockets()
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 3),
                    Entries = new List<Entry>
                    {
                        new EntryXPBottles(3),
                        new EntryEnchantedBooks(),
                        new EntryShulkerShell(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeRandom(1, 2),
                    Entries = new List<Entry>
                    {
                        new EntryDiamonds(2),
                        new EntryNetheriteScraps(2)
                    }
                },
                new Pool
                {
                    Rolls = new RangeFixed(),
                    Entries = new List<Entry>
                    {
                        new EntryWitherSkeletonSkull()
                    },
                    Conditions = new List<Condition> { new ConditionRandomChance { Chance = 0.3f } }
                },
                new PoolPotions(1, 3)
            }
        };

        static List<Level> levels = new List<Level>{ level1, level2, level3, level4, level5, level6, level7, level8, level9, level10 };
        public static Level[] getLevels()
        {
            return levels.ToArray();
        }

    }
}
