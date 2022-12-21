using System.Text.Json.Serialization;


namespace generate_loot_tables
{
    internal class Pool
    {
        public Range Rolls { get; set; }

        public List<Entry> Entries { get; set; }

        public List<Condition> Conditions { get; set; }
    }

    internal class PoolFood : Pool
    {
        public PoolFood(int min, int max)
        {
            Rolls = new RangeRandom(min, max);
            Entries = new List<Entry>
            {
                new EntryFood()
            };
        }
    }
    internal class EntryFood : EntryLootTable
    {
        public override string Name { get; set; } = "food".InNamespace();
    }

    internal class PoolPotions : Pool
    {
        public PoolPotions(int min, int max)
        {
            Rolls = new RangeRandom(min, max);
            Entries = new List<Entry>
            {
                new EntryPotions()
            };
        }
    }

    internal class EntryPotions : EntryLootTable
    {
        public override string Name { get; set; } = "potions".InNamespace();
    }
}
