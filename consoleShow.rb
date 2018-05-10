require "tco"
require "csv"

filename = "frame25.csv"

CSV.foreach(filename, :headers => false) do |row|
    row.each do |col|
        print("  ".bg [col.to_f / 5000.0 * 256, 0, 0])
    end
    print("\n")
end