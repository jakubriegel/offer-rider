if fc-list | grep -q "JetBrains Mono Medium"; then
    dot -Tpng -obig-store-schema.png offer-rider.gv
    exit 0
else
    echo "To generate the schema get JetBrains Mono typeface from https://www.jetbrains.com/lp/mono/"
    exit 1
fi
