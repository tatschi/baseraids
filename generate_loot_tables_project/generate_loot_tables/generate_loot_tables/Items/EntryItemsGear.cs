namespace generate_loot_tables.Items
{
    internal class EntryArrows : EntryItem
    {
        public EntryArrows(int factor = 1) : base()
        {
            Name = "arrow".InMCNamespace();
            AddRange(factor * 10, factor * 30);
        }
    }

    internal class EntryTotemOfUndying : EntryItem
    {
        public override string Name { get; set; } = "totem_of_undying".InMCNamespace();
    }

    internal class EntryShield : EntryItem
    {
        public override string Name { get; set; } = "shield".InMCNamespace();
    }

    #region iron unenchanted
    internal class EntryIronHelmet : EntryItem
    {
        public override string Name { get; set; } = "iron_helmet".InMCNamespace();
    }
    internal class EntryIronChestplate : EntryItem
    {
        public override string Name { get; set; } = "iron_chestplate".InMCNamespace();
    }
    internal class EntryIronLeggings : EntryItem
    {
        public override string Name { get; set; } = "iron_leggings".InMCNamespace();
    }

    internal class EntryIronBoots : EntryItem
    {
        public override string Name { get; set; } = "iron_boots".InMCNamespace();
    }

    internal class EntryIronAxe : EntryItem
    {
        public override string Name { get; set; } = "iron_axe".InMCNamespace();
    }

    internal class EntryIronPickaxe : EntryItem
    {
        public override string Name { get; set; } = "iron_pickaxe".InMCNamespace();
    }

    internal class EntryIronShovel : EntryItem
    {
        public override string Name { get; set; } = "iron_shovel".InMCNamespace();
    }

    internal class EntryIronSword : EntryItem
    {
        public override string Name { get; set; } = "iron_sword".InMCNamespace();
    }

    #endregion
    #region iron enchanted
    internal class EntryIronHelmetEnchanted : EntryItem
    {
        public EntryIronHelmetEnchanted() : base()
        {
            Name = "iron_helmet".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }
    internal class EntryIronChestplateEnchanted : EntryItem
    {
        public EntryIronChestplateEnchanted() : base()
        {
            Name = "iron_chestplate".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }
    internal class EntryIronLeggingsEnchanted : EntryItem
    {
        public EntryIronLeggingsEnchanted() : base()
        {
            Name = "iron_leggings".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }

    internal class EntryIronBootsEnchanted : EntryItem
    {
        public EntryIronBootsEnchanted() : base()
        {
            Name = "iron_boots".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }

    internal class EntryIronAxeEnchanted : EntryItem
    {
        public EntryIronAxeEnchanted() : base()
        {
            Name = "iron_axe".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }

    internal class EntryIronPickaxeEnchanted : EntryItem
    {
        public EntryIronPickaxeEnchanted() : base()
        {
            Name = "iron_pickaxe".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }

    internal class EntryIronShovelEnchanted : EntryItem
    {
        public EntryIronShovelEnchanted() : base()
        {
            Name = "iron_shovel".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }

    internal class EntryIronSwordEnchanted : EntryItem
    {
        public EntryIronSwordEnchanted() : base()
        {
            Name = "iron_sword".InMCNamespace();
            AddEnchantment();
            AddDamageDefault();
        }
    }

    #endregion

    #region diamond unenchanted
    internal class EntryDiamondHelmet : EntryItem
    {
        public override string Name { get; set; } = "diamond_helmet".InMCNamespace();
    }
    internal class EntryDiamondChestplate : EntryItem
    {
        public override string Name { get; set; } = "diamond_chestplate".InMCNamespace();
    }
    internal class EntryDiamondLeggings : EntryItem
    {
        public override string Name { get; set; } = "diamond_leggings".InMCNamespace();
    }

    internal class EntryDiamondBoots : EntryItem
    {
        public override string Name { get; set; } = "diamond_boots".InMCNamespace();
    }

    internal class EntryDiamondAxe : EntryItem
    {
        public override string Name { get; set; } = "diamond_axe".InMCNamespace();
    }

    internal class EntryDiamondPickaxe : EntryItem
    {
        public override string Name { get; set; } = "diamond_pickaxe".InMCNamespace();
    }

    internal class EntryDiamondShovel : EntryItem
    {
        public override string Name { get; set; } = "diamond_shovel".InMCNamespace();
    }

    internal class EntryDiamondSword : EntryItem
    {
        public override string Name { get; set; } = "diamond_sword".InMCNamespace();
    }

    #endregion
    #region diamond enchanted
    internal class EntryDiamondHelmetEnchanted : EntryItem
    {
        public EntryDiamondHelmetEnchanted(int factor = 1) : base()
        {
            Name = "diamond_helmet".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }
    internal class EntryDiamondChestplateEnchanted : EntryItem
    {
        public EntryDiamondChestplateEnchanted(int factor = 1) : base()
        {
            Name = "diamond_chestplate".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }
    internal class EntryDiamondLeggingsEnchanted : EntryItem
    {
        public EntryDiamondLeggingsEnchanted(int factor = 1) : base()
        {
            Name = "diamond_leggings".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryDiamondBootsEnchanted : EntryItem
    {
        public EntryDiamondBootsEnchanted(int factor = 1) : base()
        {
            Name = "diamond_boots".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryDiamondAxeEnchanted : EntryItem
    {
        public EntryDiamondAxeEnchanted(int factor = 1) : base()
        {
            Name = "diamond_axe".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryDiamondPickaxeEnchanted : EntryItem
    {
        public EntryDiamondPickaxeEnchanted(int factor = 1) : base()
        {
            Name = "diamond_pickaxe".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryDiamondShovelEnchanted : EntryItem
    {
        public EntryDiamondShovelEnchanted(int factor = 1) : base()
        {
            Name = "diamond_shovel".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryDiamondSwordEnchanted : EntryItem
    {
        public EntryDiamondSwordEnchanted(int factor = 1) : base()
        {
            Name = "diamond_sword".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    #endregion

    #region netherite unenchanted
    internal class EntryNetheriteHelmet : EntryItem
    {
        public override string Name { get; set; } = "netherite_helmet".InMCNamespace();
    }
    internal class EntryNetheriteChestplate : EntryItem
    {
        public override string Name { get; set; } = "netherite_chestplate".InMCNamespace();
    }
    internal class EntryNetheriteLeggings : EntryItem
    {
        public override string Name { get; set; } = "netherite_leggings".InMCNamespace();
    }

    internal class EntryNetheriteBoots : EntryItem
    {
        public override string Name { get; set; } = "netherite_boots".InMCNamespace();
    }

    internal class EntryNetheriteAxe : EntryItem
    {
        public override string Name { get; set; } = "netherite_axe".InMCNamespace();
    }

    internal class EntryNetheritePickaxe : EntryItem
    {
        public override string Name { get; set; } = "netherite_pickaxe".InMCNamespace();
    }

    internal class EntryNetheriteShovel : EntryItem
    {
        public override string Name { get; set; } = "netherite_shovel".InMCNamespace();
    }

    internal class EntryNetheriteSword : EntryItem
    {
        public override string Name { get; set; } = "netherite_sword".InMCNamespace();
    }

    #endregion
    #region netherite enchanted
    internal class EntryNetheriteHelmetEnchanted : EntryItem
    {
        public EntryNetheriteHelmetEnchanted(int factor = 1) : base()
        {
            Name = "netherite_helmet".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }
    internal class EntryNetheriteChestplateEnchanted : EntryItem
    {
        public EntryNetheriteChestplateEnchanted(int factor = 1) : base()
        {
            Name = "netherite_chestplate".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }
    internal class EntryNetheriteLeggingsEnchanted : EntryItem
    {
        public EntryNetheriteLeggingsEnchanted(int factor = 1) : base()
        {
            Name = "netherite_leggings".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryNetheriteBootsEnchanted : EntryItem
    {
        public EntryNetheriteBootsEnchanted(int factor = 1) : base()
        {
            Name = "netherite_boots".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryNetheriteAxeEnchanted : EntryItem
    {
        public EntryNetheriteAxeEnchanted(int factor = 1) : base()
        {
            Name = "netherite_axe".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryNetheritePickaxeEnchanted : EntryItem
    {
        public EntryNetheritePickaxeEnchanted(int factor = 1) : base()
        {
            Name = "netherite_pickaxe".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryNetheriteShovelEnchanted : EntryItem
    {
        public EntryNetheriteShovelEnchanted(int factor = 1) : base()
        {
            Name = "netherite_shovel".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    internal class EntryNetheriteSwordEnchanted : EntryItem
    {
        public EntryNetheriteSwordEnchanted(int factor = 1) : base()
        {
            Name = "netherite_sword".InMCNamespace();
            AddEnchantment();
            AddDamageDefault(factor);
        }
    }

    #endregion

    internal class EntryElytra : EntryItem
    {
        public override string Name { get; set; } = "elytra".InMCNamespace();
    }

    internal class EntryFireworkRockets : EntryItem
    {
        public EntryFireworkRockets() : base()
        {
            Name = "firework_rocket".InMCNamespace();
            AddRange(10, 30);
        }
    }
}
