package com.squirrel7ang.umlgenerator;

import com.oocourse.library2.Trigger;

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
}
