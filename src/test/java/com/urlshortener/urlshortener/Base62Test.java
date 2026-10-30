package com.urlshortener.urlshortener;

import com.urlshortener.urlshortener.util.Base62;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base62Test {

    @Test
    void encodeKnownValue() {
        assertEquals("21", Base62.encode(125));
    }

    @Test
    void decodeReversesEncode() {
        long id = 987654321L;
        assertEquals(id, Base62.decode(Base62.encode(id)));
    }
}