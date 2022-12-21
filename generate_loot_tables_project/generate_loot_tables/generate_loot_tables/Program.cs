using generate_loot_tables;
using System.Text.Json;
using System.Text.Json.Serialization;

public class Program
{
    private static string pathLootTables = "C:\\Users\\Natas\\Documents\\GitHub\\baseraids\\generated_loot_tables";
    public static string Namespace = "baseraids";
    public static string MCNamespace = "minecraft";
    public static void Main()
    {
        SelectMode();        
    }

    public static void SelectMode()
    {
        new Decider()
        {
            Description = "Select mode",
            DecisionItems = new Dictionary<string, Action>
            {
                {"Exit", () => Environment.Exit(0)},
                {"Generate loot tables", () => GenerateLootTables()},
                {"Add items", () => AddItemsSelection() }
            }
        }.RunDecision();
    }
    public static void GenerateLootTables()
    {
        Console.WriteLine("Generating loot tables");
        foreach(Level level in Levels.getLevels())
        {
            var options = new JsonSerializerOptions
            {
                WriteIndented = true,
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
            };
            string jsonLevel = JsonSerializer.Serialize(level, options);
            File.WriteAllText(Path.Combine(pathLootTables, level.Name), jsonLevel);
        }
        Console.WriteLine("Finished generating loot tables");

        SelectMode();
    }

    public static void AddItemsSelection()
    {
        new Decider()
        {
            Description = "Select item",
            DecisionItems = new Dictionary<string, Action>
            {
                {"Exit", () => Environment.Exit(0)},
                {"Back", () => SelectMode()},
                {"Add EntryItem", () => AddEntryItem()},
                {"Add EntryItemWithRange", () => AddEntryItemWithRange()}
            }
        }.RunDecision();
        AddItemsSelection();
    }

    private static void AddEntryItem()
    {
        string fileName = "items";
        Console.WriteLine("Enter name of new item (i.e. blue_ice): ");
        string? itemName = Console.ReadLine();

        if (itemName == null)
        {
            return;
        }

        string className = itemName.SnakeCaseToPascalCase();

        string content = $"internal class Entry{className} : EntryItem\n{{\n\tpublic Entry{className}() : base()\n\t{{\n\t\tName = \"{itemName}\".InMCNamespace();\n\t}}\n}}\n";

        string path = Path.Combine(pathLootTables, fileName + ".cs");
        File.AppendAllText(path, content);
    }

    private static void AddEntryItemWithRange()
    {
        string fileName = "items";
        Console.WriteLine("Enter name of new item (i.e. blue_ice): ");
        string? itemName = Console.ReadLine();
        if (itemName == null)
        {
            return;
        }
        Console.WriteLine("Enter min: ");
        if(!int.TryParse(Console.ReadLine(), out int minVal)) { return; }
        Console.WriteLine("Enter max: ");
        if(!int.TryParse(Console.ReadLine(), out int maxVal)) { return; }

        string className = itemName.SnakeCaseToPascalCase();

        string content = $"internal class Entry{className} : EntryItem\n{{\n\tpublic Entry{className}() : base()\n\t{{\n\t\tName = \"{itemName}\".InMCNamespace();\n\t\tAddRange({minVal}, {maxVal});\n\t}}\n}}\n";

        string path = Path.Combine(pathLootTables, fileName + ".cs");
        File.AppendAllText(path, content);
    }

    

}