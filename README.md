## What is this?

this is a simple staruml generator build for BUAA OO Unit-4in order to avoid stupid 
typing mistakes during mid-tests. It can generate staruml file based on java source 
file.

this project is based on [JavaParser](https://github.com/javaparser/javaparser) and org.JSON, and import them with Maven.

## Version Info

updated to v1.1

## Feature Supported

- class diagrams
- Statechart diagram
- class and interface declaration
- attributes and operations of class or declaration and their basic modifier.
- Realization, Generalization and Aggregation relationships are supported.
- better Modifier
- InitState and FinalState supported. 

the following feature is not yet supported
- enumerable declaration.
- sequence diagram support

## Usage

just build it and run Main.main.

## FAQ

> 1. where's the diagram? I see a blank sheet.

You will need to drag them from the frame on the left explorer to your workspace, 
and design on your own. This project only meant to avoid making stupid
typing mistakes. 

As for state chart and other chart, you will need to double-click on them on the
explorer on your left, and choose the diagram you want to design by double-clicking
them on the left.

> 2. New to Maven?

***Notice: the release is only updated to v1.0.***

you can also download Release at the [release](https://github.com/Squirrel7ang/Simple-Staruml-Generater/releases) page. 

> 3. How to use InitState and FinalState. 

just change the name of the state into "InitState" or "FinalState" will do. 

> 4. unable to identify Trigger and Triggers?

umlgenerator only support the Trigger and Triggers annotation that is written in 
key-value form. For example, your annotation should be like this:

```java
public class Library {
    @Trigger(from = "State1", to = {"State2", "State3"})
    @Trigger(from = "State2", to = "State3")
    public void method1() {
        
    }
    
    @Triggers(value = {
        @Trigger(from = "State2", to = "State3"),
        @Trigger(from = "State3", to = {"State2", "State1"})
    })
    public void method2() {
        
    }
}
```

Also, the oocourse's TAs also suggest don't use `Triggers` and `SendMessages` 
annotation directly.
