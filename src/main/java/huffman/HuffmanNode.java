package huffman;

public class HuffmanNode implements Comparable<HuffmanNode> {
    public int frequency;
    public Byte data; // Null para nodos internos
    public HuffmanNode left, right;

    public HuffmanNode(Byte data, int frequency) {
        this.data = data;
        this.frequency = frequency;
    }

    public HuffmanNode(HuffmanNode left, HuffmanNode right) {
        this.data = null;
        this.frequency = left.frequency + right.frequency;
        this.left = left;
        this.right = right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return Integer.compare(this.frequency, o.frequency);
    }
}
