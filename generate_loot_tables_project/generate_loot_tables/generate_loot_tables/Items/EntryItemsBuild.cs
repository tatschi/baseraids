namespace generate_loot_tables.Items
{
    internal class EntryWoodLogs : EntryLootTable
    {
        public EntryWoodLogs(int factor = 1) : base()
        {
            Name = "wood_logs".InNamespace();
            AddRange(factor * 15, factor * 30);
        }
    }

    internal class EntrySand : EntryItem
    {
        public EntrySand(int factor = 1) : base()
        {
            Name = "sand".InMCNamespace();
            AddRange(factor * 20, factor * 40);
        }
    }

    internal class EntryClayBalls : EntryItem
    {
        public EntryClayBalls(int factor = 1) : base()
        {
            Name = "clay_ball".InMCNamespace();
            AddRange(factor * 30, factor * 100);
        }
    }

    internal class EntryGlowstoneDust : EntryItem
    {
        public EntryGlowstoneDust(int factor = 1) : base()
        {
            Name = "glowstone_dust".InMCNamespace();
            AddRange(factor * 15, factor * 30);
        }
    }

    internal class EntryLapisLazuli : EntryItem
    {
        public EntryLapisLazuli(int factor = 1) : base()
        {
            Name = "lapis_lazuli".InMCNamespace();
            AddRange(factor * 30, factor * 80);
        }
    }
    internal class EntryQuartz : EntryItem
    {
        public EntryQuartz(int factor = 1) : base()
        {
            Name = "quartz".InMCNamespace();
            AddRange(factor * 30, factor * 80);
        }
    }
    internal class EntryLantern : EntryItem
    {
        public EntryLantern(int factor = 1) : base()
        {
            Name = "lantern".InMCNamespace();
            AddRange(factor * 4, factor * 10);
        }
    }
    internal class EntryVine : EntryItem
    {
        public EntryVine() : base()
        {
            Name = "vine".InMCNamespace();
        }
    }
    internal class EntryBlueIce : EntryItem
    {
        public EntryBlueIce(int factor = 1) : base()
        {
            Name = "blue_ice".InMCNamespace();
            AddRange(factor * 5, factor * 64);
        }
    }

    internal class EntryObsidian : EntryItem
    {
        public EntryObsidian(int factor = 1) : base()
        {
            Name = "obsidian".InMCNamespace();
            AddRange(factor * 3, factor * 15);
        }
    }


}
