package dev.paperswap.runtime.livepatch;

/**
 * One detected difference.
 *
 * @param kind   category
 * @param detail affected member, e.g. {@code execute(CommandSender, String, String[]) : boolean}
 */
public record ClassChange(ChangeKind kind, String detail) {

    @Override
    public String toString() {
        return this.kind.description() + (this.detail == null || this.detail.isEmpty() ? "" : ": " + this.detail);
    }
}
