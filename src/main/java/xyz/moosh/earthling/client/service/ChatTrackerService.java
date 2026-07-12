/*
 * Earthling
 * Copyright (c) 2025 Moosh
 *
 * Earthling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Earthling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Earthling. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package xyz.moosh.earthling.client.service;

import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.event.impl.ChatReceiveEvent;
import xyz.moosh.earthling.client.model.ChatChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatTrackerService extends Service {


    private ChatChannel current = ChatChannel.GLOBAL;

    private boolean party = false;


    private final Pattern channelPattern =
            Pattern.compile("([A-Za-z]+)\\s*\\(write\\)");


    private EventBus.EventListener<ChatReceiveEvent> listener;



    public ChatTrackerService(EventBus eventBus) {
        super("chat_tracker", eventBus);
    }



    @Override
    public void init() {

        listener = eventBus.subscribe(
                ChatReceiveEvent.class,
                event -> track(event.getMessage().getString())
        );
    }



    @Override
    public void shutdown() {

        if(listener != null) {

            eventBus.unsubscribe(
                    ChatReceiveEvent.class,
                    listener
            );

        }
    }




    private void track(String message) {

        if(message == null || message.isEmpty())
            return;


        System.out.println("[Earthling ChatTracker] " + message);


        /*
         * Example:
         * You are currently in Nation (write)
         */
        if(message.contains("You are currently in")) {

            Matcher matcher = channelPattern.matcher(message);

            if(matcher.find()) {

                String channel = matcher.group(1);

                current = ChatChannel.get(channel);

                System.out.println(
                        "[Earthling ChatTracker] Switched to: "
                                + current.getName()
                );
            }
        }



        /*
         * Example:
         * » You have joined the channel: Nation!
         */
        if(message.contains("You have joined the channel")) {


            int index = message.indexOf(":");


            if(index != -1) {

                String channel =
                        message.substring(index + 1)
                                .replace(".", "")
                                .replace("!", "")
                                .trim();


                current = ChatChannel.get(channel);


                System.out.println(
                        "[Earthling ChatTracker] Joined: "
                                + current.getName()
                );
            }
        }



        if(message.contains(
                "automatically delivered to the Party chat"
        )) {

            party = true;

        }



        if(message.contains(
                "no longer be automatically delivered"
        )
                ||
                message.contains(
                        "left that party"
                )) {

            party = false;

        }
    }





    public ChatChannel getChannel() {

        return current;

    }




    public boolean isParty() {

        return party;

    }
}