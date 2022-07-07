"""
Creates a minecraft loot table from multiple .json files that contain one pool of items each.
Place all pool-files that you want to add to the final loot table into a subfolder "gen_pools" relative to this file.
The output file will be located in the same directory as this file and called "raid_level_<level>" with the <level> being the
given Raid level.
"""
import os
import glob


# Prompt for level and create file name of output file
level = input("Enter raid level: ")
output_filename = "raid_level_{}.json".format(level)

# Get all .json files from subfolder
print("Fetching .json files")
os.chdir(os.path.dirname(__file__) + "\\gen_pools")
pools = glob.glob("*.json").copy()


# Create content of loot table
print("Creating content of loot table")

prefix = "{\n\t\"pools\": ["
postfix = "\n\t]\n}"

output = prefix

for pool in pools:
    f = open(pool, 'r')
    file_content = f.read()
    # add tabs to each line of the file
    file_content = "\t\t" + file_content
    file_content = '\t\t'.join(file_content.splitlines(True))
    f.close()
    output += "\n"
    output += file_content
    output += ","

output = output[:-1] # remove last ","
output += postfix

# Create output file
print("Creating output file")
output_file = open("..\\" + output_filename, "w")
output_file.write(output)
output_file.close()

print("Finished")