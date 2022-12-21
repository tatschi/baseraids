namespace generate_loot_tables.Items
{
    internal class EntryLeather : EntryItem
    {
        public EntryLeather() : base()
        {
            Name = "leather".InMCNamespace();
            AddRange(5, 20);
        }
    }

    internal class EntryEmerald : EntryItem
    {
        public EntryEmerald(int factor = 1) : base()
        {
            Name = "emerald".InMCNamespace();
            AddRange(factor * 5, factor * 15);
        }
    }

    internal class EntryCoal : EntryItem
    {
        public EntryCoal() : base()
        {
            Name = "coal".InMCNamespace();
            AddRange(20, 40);
        }
    }

    internal class EntryIronIngots : EntryItem
    {
        public EntryIronIngots(int factor = 1) : base()
        {
            Name = "iron_ingot".InMCNamespace();
            AddRange(factor * 5, factor * 20);
        }
    }

    internal class EntryGoldIngots : EntryItem
    {
        public EntryGoldIngots() : base()
        {
            Name = "gold_ingot".InMCNamespace();
            AddRange(5, 10);
        }
    }

    internal class EntryDiamonds : EntryItem
    {
        public EntryDiamonds(int factor = 1) : base()
        {
            Name = "diamond".InMCNamespace();
            AddRange(1, factor * 3);
        }
    }

    internal class EntryNetheriteScraps : EntryItem
    {
        public EntryNetheriteScraps(int factor = 1) : base()
        {
            Name = "netherite_scrap".InMCNamespace();
            AddRange(1, factor * 10);
        }
    }
}
