namespace generate_loot_tables
{
    public static class StringExtensions
    {
        public static string InNamespace(this string name)
        {
            return Program.Namespace + ":" + name;
        }

        public static string InMCNamespace(this string name)
        {
            return Program.MCNamespace + ":" + name;
        }

        public static string ToPascalCase(this string input)
        {
            return char.ToUpper(input[0]) + input.Substring(1);
        }

        public static string SnakeCaseToPascalCase(this string input)
        {
            string[] splitWords = input.Split("_");
            string result = "";
            foreach (var word in splitWords)
            {
                result += char.ToUpper(word[0]) + word.Substring(1);
            }

            return result;
        }
    }
}
