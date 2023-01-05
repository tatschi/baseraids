using System.Text.Json;
using System.Text.Json.Serialization;

namespace generate_loot_tables
{
    [JsonConverter(typeof(ConditionConverter))]
    internal class Condition
    {
        [JsonPropertyName("condition")]
        public virtual string Cond { get; set; }
    }

    internal class ConditionRandomChance : Condition
    {
        [JsonPropertyName("condition")]
        public override string Cond { get; set; } = "minecraft:random_chance";
        public float Chance { get; set; }
    }

    internal class ConditionConverter : JsonConverter<Condition>
    {
        public override Condition? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            throw new NotImplementedException();
        }

        public override void Write(Utf8JsonWriter writer, Condition value, JsonSerializerOptions options)
        {
            if (value is ConditionRandomChance randomChance)
            {
                JsonSerializer.Serialize(writer, randomChance, options);
            }
        }
    }
}
