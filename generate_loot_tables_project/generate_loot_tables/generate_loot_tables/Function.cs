using System.Text.Json;
using System.Text.Json.Serialization;

namespace generate_loot_tables
{

    [JsonConverter(typeof(FunctionConverter))]
    internal class Function
    {
        [JsonPropertyName("function")]
        public virtual string Func { get; set; }
    }
    internal class FunctionSetCount : Function
    {
        [JsonPropertyName("function")]
        public override string Func { get; set; } = "minecraft:set_count";

        public Range Count { get; set; }
    }

    internal class FunctionEnchantWithLevels : Function
    {
        [JsonPropertyName("function")]
        public override string Func { get; set; } = "minecraft:enchant_with_levels";

        public int Levels { get; set; }

        public bool Treasure { get; set; } = true;
    }

    internal class FunctionSetDamage : Function
    {
        [JsonPropertyName("function")]
        public override string Func { get; set; } = "minecraft:set_damage";

        public Range Damage { get; set; }
    }

    internal class FunctionConverter : JsonConverter<Function>
    {
        public override Function? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            throw new NotImplementedException();
        }

        public override void Write(Utf8JsonWriter writer, Function value, JsonSerializerOptions options)
        {
            if (value is FunctionSetCount setCount)
            {
                JsonSerializer.Serialize(writer, setCount, options);
            }
        }
    }

}
