package com.avolution.actor.system.actor;

public interface IDeadLetterActorMessage {

    class DeadLetter implements IDeadLetterActorMessage{

        private final Object message;
        private final String sender;
        private final String recipient;
        private final String timestamp;
        private final String messageType;
        private final int retryCount;

        public DeadLetter(Object message, String sender, String recipient, String timestamp, String messageType, int retryCount){
            this.message = message;
            this.sender = sender;
            this.recipient = recipient;
            this.timestamp = timestamp;
            this.messageType = messageType;
            this.retryCount = retryCount;
        }

        public Object getMessage(){
            return message;
        }

        public String getSender(){
            return sender;
        }

        public String getRecipient(){
            return recipient;
        }

        public String getTimestamp(){
            return timestamp;
        }

        public String getMessageType(){
            return messageType;
        }

        public int getRetryCount(){
            return retryCount;
        }
    }

}
