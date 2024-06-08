package com.squirrel7ang.umlgenerator;

import com.oocourse.library3.annotation.SendMessage;
import com.oocourse.library3.annotation.Trigger;
import com.oocourse.library3.annotation.Triggers;

import java.util.ArrayList;

public class TestClass {
    private Boolean privateMember;

    public TestClass() {
        privateMember = true;
    }

    @Trigger(from = "false", to = "true")
    @Trigger(from = "InitState", to = {"state1", "state2"})
    // Example: comments cannot be written in this way
    public boolean setTrue() {
        if (!privateMember) {
            privateMember = true;
            return true;
        }
        else {
            return false;
        }
    }

    @SendMessage(from = "hello", to = "world")
    private ArrayList<String> hello() {
        ArrayList<String> ret = new ArrayList<>();
        ret.add("hello world");
        return ret;
    }

    @SendMessage(from = "sender", to = "receiver")
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
