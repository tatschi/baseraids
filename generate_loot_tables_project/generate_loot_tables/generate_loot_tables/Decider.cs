
namespace generate_loot_tables
{
    internal class Decider
    {
        public string Description { get; set; }
        public Dictionary<string, Action> DecisionItems { get; set; }

        public void RunDecision()
        {
            while (true)
            {
                if (TryRunDecision())
                {
                    return;
                }
                Console.WriteLine("Error: Try again");
            }

        }

        public bool TryRunDecision()
        {
            Console.Clear();
            if (Description == null || DecisionItems == null)
            {
                return false;
            }
            Console.WriteLine(Description);
            for (int i = 0; i < DecisionItems.Count; i++)
            {
                KeyValuePair<string, Action> pair = DecisionItems.ElementAt(i);
                Console.WriteLine("(" + i + ") " + pair.Key);
            }

            string? input = Console.ReadLine();
            if (input == null || input.Length == 0 || !int.TryParse(input, out int value))
            {
                return false;
            }

            try
            {
                KeyValuePair<string, Action> pair = DecisionItems.ElementAt(value);
                pair.Value.Invoke();
                return true;
            }
            catch (ArgumentOutOfRangeException)
            {
                return false;
            }
        }
    }
}
