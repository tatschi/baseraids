namespace generate_loot_tables.Items
{
    internal class EntryExplorerMap : EntryLootTable
    {
        public override string Name { get; set; } = "explorer_maps".InNamespace();
    }

    internal class EntryXPBottles : EntryItem
    {
        public EntryXPBottles(float factor = 1) : base()
        {
            Name = "experience_bottle".InMCNamespace();
            AddRange(5, factor * 15);
        }
    }

    internal class EntrySaddle : EntryItem
    {
        public override string Name { get; set; } = "saddle".InMCNamespace();
    }
    internal class EntryNameTag : EntryItem
    {
        public override string Name { get; set; } = "name_tag".InMCNamespace();
    }

    internal class EntryEnchantedBooks : EntryItem
    {
        public EntryEnchantedBooks() : base()
        {
            Name = "book".InMCNamespace();
            AddRange(1, 3);
            AddEnchantment();
        }
    }

    internal class EntryEnderPearl : EntryItem
    {
        public EntryEnderPearl() : base()
        {
            Name = "ender_pearl".InMCNamespace();
            AddRange(1, 5);
        }
    }

    internal class EntryBlazeRod : EntryItem
    {
        public EntryBlazeRod() : base()
        {
            Name = "blaze_rod".InMCNamespace();
            AddRange(1, 10);
        }
    }
    internal class EntryShulkerShell : EntryItem
    {
        public EntryShulkerShell(float factor = 1) : base()
        {
            Name = "shulker_shell".InMCNamespace();
            AddRange(1, factor * 3);
        }
    }
}
