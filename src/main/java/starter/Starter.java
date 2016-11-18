package starter;

import translator.JSONTranslator;

public class Starter {
       public static void main(String[] argv) throws Exception {
        JSONTranslator translator = new JSONTranslator();
        translator.init();
    }
}
