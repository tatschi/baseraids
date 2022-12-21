namespace generate_loot_tables.Items
{
    internal class EntryZombieHead : EntryItem
    {
        public override string Name { get; set; } = "zombie_head".InMCNamespace();
    }
    internal class EntrySkeletonSkull : EntryItem
    {
        public override string Name { get; set; } = "skeleton_skull".InMCNamespace();
    }

    internal class EntryWitherSkeletonSkull : EntryItem
    {
        public override string Name { get; set; } = "wither_skeleton_skull".InMCNamespace();
    }
}
