package org.geogit.web.api;

/**
 *
 */
public abstract class CommandResponse {

    public abstract void write(ResponseWriter out) throws Exception;

    public static CommandResponse warning(String message) {
        return new ErrorLiteral("warning", message);
    }

    public static CommandResponse error(String message) {
        return new ErrorLiteral("error", message);
    }

    static class ErrorLiteral extends CommandResponse {
        private final String[] items;

        public ErrorLiteral(String... items) {
            this.items = items;
        }

        @Override
        public void write(ResponseWriter out) throws Exception {
            out.start(false);
            for (int i = 0; i < items.length; i += 2) {
                out.writeElement(items[i], items[i + 1]);
            }
            out.finish();
        }

    }

}
