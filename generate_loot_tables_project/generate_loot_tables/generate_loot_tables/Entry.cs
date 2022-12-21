using System.Text.Json.Serialization;

namespace generate_loot_tables
{
    internal class Entry
    {
        public virtual string Type { get; set; }

        public virtual string Name { get; set; }

        public virtual int Weight { get; set; } = 1;

        public virtual List<Function> Functions { get; set; }

        public void AddRange(float min, float max)
        {
            if (Functions == null)
            {
                Functions = new List<Function>();
            }
            Functions.Add(
                new FunctionSetCount { Count = new RangeRandom(min, max) }
            );
        }

        public void AddEnchantment()
        {
            if (Functions == null)
            {
                Functions = new List<Function>();
            }
            Functions.Add(
                new FunctionEnchantWithLevels { Levels = 30 }
            );
        }

        public void AddDamage(float min, float max)
        {
            if (Functions == null)
            {
                Functions = new List<Function>();
            }
            Functions.Add(
                new FunctionSetDamage { Damage = new RangeRandom(min, max) }
            );
        }

        public void AddDamageDefault(int factor = 1)
        {
            AddDamage(0.05f, factor * 0.2f);
        }

    }

    internal class EntryLootTable : Entry
    {
        public override string Type { get; set; } = "minecraft:loot_table";
    }

    internal class EntryItem : Entry
    {
        public override string Type { get; set; } = "minecraft:item";
    }
}
