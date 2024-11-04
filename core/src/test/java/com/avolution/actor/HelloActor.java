//package com.avolution.actor;
//
//public class HelloActor extends Actor{
//    @Override
//    protected void receive(Object message) {
//        if (message instanceof String msg) {
//            System.out.println("Received: " + msg);
//            if (msg.contains("Back")){
//                getSender().tellMessage("Hello ACS! 123", self());
//            }
//        }
//    }
//}
