package com.squirrel7ang.umlgenerator;

import com.oocourse.library2.Trigger;
import com.oocourse.library2.Triggers;

public class TestClass {
    private Boolean privateMember;

    public TestClass() {
        privateMember = true;
    }

    @Trigger(from = "false", to = "true")
    @Trigger(from = "InitState", to = {"state1", "state2"})
    public boolean setTrue() {
        if (!privateMember) {
            privateMember = true;
            return true;
        }
        else {
            return false;
        }
    }

    @Triggers(value = {
        @Trigger(from = "false", to = "true"),
        @Trigger(from = "InitState", to = {"state1", "state2"})
    })
    private boolean setFalse() {
        if (privateMember) {
            privateMember = false;
            return true;
        }
        else {
            return false;
        }
    }
}
