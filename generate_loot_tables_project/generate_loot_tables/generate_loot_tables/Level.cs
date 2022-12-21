using System.Text.Json.Serialization;

namespace generate_loot_tables
{
    internal class Level
    {
        [JsonIgnore]
        public string Name { get; set; }

        public List<Pool> Pools { get; set; }
    }
}
