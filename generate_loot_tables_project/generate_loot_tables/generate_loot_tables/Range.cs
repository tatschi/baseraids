using System.Text.Json;
using System.Text.Json.Serialization;

namespace generate_loot_tables
{
    [JsonConverter(typeof(RangeConverter))]
    public class Range {}

    [JsonConverter(typeof(RangeFixedConverter))]
    public class RangeFixed : Range
    {
        public int FixedNum { get; set; } = 1;
    }

    public class RangeRandom : Range
    {
        public float Min { get; set; }

        public float Max { get; set; }

        public RangeRandom(float min, float max)
        {
            Min = min;
            Max = max;
        }
    }

    public class RangeConverter : JsonConverter<Range>
    {
        public override Range? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            throw new NotImplementedException();
        }

        public override void Write(Utf8JsonWriter writer, Range value, JsonSerializerOptions options)
        {
            if (value is RangeFixed rangeFixed)
            {
                JsonSerializer.Serialize(writer, rangeFixed, options);
            }
            else if (value is RangeRandom rangeRandom)
            {
                JsonSerializer.Serialize(writer, rangeRandom, options);
            }
        }
    }

    public class RangeFixedConverter : JsonConverter<RangeFixed>
    {
        public override RangeFixed? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            throw new NotImplementedException();
        }

        public override void Write(Utf8JsonWriter writer, RangeFixed value, JsonSerializerOptions options)
        {
            writer.WriteNumberValue(value.FixedNum);
        }
    }
}
