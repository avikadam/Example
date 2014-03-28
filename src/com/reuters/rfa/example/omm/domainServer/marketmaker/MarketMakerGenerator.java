package com.reuters.rfa.example.omm.domainServer.marketmaker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.reuters.rfa.common.Token;
import com.reuters.rfa.example.framework.prov.ProvDomainMgr;
import com.reuters.rfa.example.omm.domainServer.DataGenerator;
import com.reuters.rfa.example.omm.domainServer.DataStreamItem;
import com.reuters.rfa.example.utility.CommandLine;
import com.reuters.rfa.omm.OMMMapEntry;
import com.reuters.rfa.omm.OMMMsg;

/**
 * MarketMakerGenerator provides a simple algorithm to generate data for Market
 * Maker domain. It can be used to create MarketMakerStreamItems.
 * 
 * <p>
 * <b>How to generate response data.</b>
 * </p>
 * 
 * Unique MarketMakers(key) are generated by using random the four-character
 * string. Each Market Maker works a BID and ASK order. The source of the market
 * maker and market maker identifier are randomly generated at the first time
 * and stay constant throughout program execution. The prices and order sizes
 * for both ASK and BID are chosen randomly as well as the action (update, add,
 * or delete). The time of the order is the current time in which response data
 * be created.
 * 
 * @see MarketMakerStreamItem
 */
public class MarketMakerGenerator implements DataGenerator
{

    private List<OrderEntry> _orderEntries;
    private List<OrderEntry> _modifiedEntries;
    private int _id;
    private int _basePrice;

    // the maximum number of caches
    private int _maxEntries = 50;
    private Random _random;

    private int _deleteFactor = 10; // 10% chance of deleting a current entry
    // private int _addFactor = 10; // 10% chance of adding a current entry
    private int _updateFactor = 80; // 80% chance of updating a current entry

    /**
     * Represents a single order entry
     */
    public static class OrderEntry implements Cloneable
    {

        public byte action; // UPDATE, ADD, DELETE
        public String makerId;
        public int bidPrice;
        public long bidSize;
        public int askPrice;
        public long askSize;
        public int mkSource;
        public String mkName;
        public long quotim;
        public int priCode;

        public Object clone()
        {
            OrderEntry newEntry = new OrderEntry();
            newEntry.makerId = makerId;
            newEntry.action = action;
            newEntry.bidPrice = bidPrice;
            newEntry.bidSize = bidSize;
            newEntry.askPrice = askPrice;
            newEntry.askSize = askSize;
            newEntry.mkSource = mkSource;
            newEntry.mkName = mkName;
            newEntry.quotim = quotim;
            newEntry.priCode = priCode;
            return newEntry;
        }
    }

    public MarketMakerGenerator()
    {

        _orderEntries = new ArrayList<OrderEntry>(50);
        _modifiedEntries = new ArrayList<OrderEntry>(50);
        _random = new Random();
        _basePrice = (_random.nextInt(10) + 10) * 100;
        _id = 0;

        for (int i = 0; i < 25; i++)
        {
            _orderEntries.add(getNewOrder());
        }
    }

    public DataStreamItem createStreamItem(ProvDomainMgr mgr, Token token, OMMMsg msg)
    {
        boolean encodeDataDef = CommandLine.booleanVariable("MARKET_MAKER_encodeDataDef");

        MarketMakerStreamItem streamItem = new MarketMakerStreamItem(this, mgr, token, msg);
        streamItem.setEncodeDataDef(encodeDataDef);
        return streamItem;
    }

    public Object[] getInitialEntries()
    {
        return _orderEntries.toArray();
    }

    public Object[] getNextEntries()
    {
        return _modifiedEntries.toArray();
    }

    public void generateUpdatedEntries()
    {

        _modifiedEntries.clear();
        HashSet<String> newEntries = new HashSet<String>();

        for (int i = 0; i < 5; i++)
        {
            OrderEntry orderEntry = null;
            int action = _random.nextInt(100);

            if (_orderEntries.size() == 0 || action >= _deleteFactor + _updateFactor)
            {
                // Action Add
                if (_orderEntries.size() >= _maxEntries)
                {
                    continue;
                }
                orderEntry = getNewOrder();
                _orderEntries.add(orderEntry);

            }
            else if (action < _deleteFactor)
            {
                // Action Delete
                int index = _random.nextInt(_orderEntries.size());
                orderEntry = (OrderEntry)_orderEntries.get(index);

                if (newEntries.contains(orderEntry.makerId))
                {
                    continue;
                }
                orderEntry.action = OMMMapEntry.Action.DELETE;
                _orderEntries.remove(orderEntry);

            }
            else
            {
                // Action Update
                int index = _random.nextInt(_orderEntries.size());
                OrderEntry updateEntry = (OrderEntry)_orderEntries.get(index);

                if (newEntries.contains(updateEntry.makerId))
                {
                    continue;
                }
                updateOrder(updateEntry);
                orderEntry = (OrderEntry)updateEntry.clone();
                orderEntry.action = OMMMapEntry.Action.UPDATE;
            }

            newEntries.add(orderEntry.makerId);
            _modifiedEntries.add(orderEntry);
        }
    }

    private OrderEntry getNewOrder()
    {

        OrderEntry orderEntry = new OrderEntry();
        String tempMaker = String.valueOf(new char[] { (char)(_random.nextInt(26) + 65),
                (char)(_random.nextInt(26) + 65), (char)(_random.nextInt(26) + 65),
                (char)(_random.nextInt(26) + 65) });
        orderEntry.makerId = tempMaker;
        orderEntry.mkName = tempMaker
                + (_random.nextBoolean() ? (_random.nextBoolean() ? " & Co., Inc." : " Group, Inc.")
                        : ", Inc.");
        orderEntry.action = OMMMapEntry.Action.ADD;

        _id++;

        int percentChange = _random.nextInt(10);
        int change = ((_basePrice * percentChange) / 100);

        orderEntry.bidPrice = _basePrice - change;
        orderEntry.askPrice = _basePrice + change;

        orderEntry.bidSize = 100 + _random.nextInt(100);
        orderEntry.askSize = 100 + _random.nextInt(100);

        orderEntry.mkSource = _random.nextInt(4) + 1;
        long currentTime = System.currentTimeMillis();
        orderEntry.quotim = currentTime % (60 * 60 * 24 * 1000); // need midnight
        orderEntry.priCode = 60;

        return orderEntry;
    }

    private void updateOrder(OrderEntry orderEntry)
    {

        int percentChange = _random.nextInt(15) + 1;
        int change = ((orderEntry.bidPrice * percentChange) / 100);

        boolean move = _random.nextBoolean();
        if (move)
        {
            orderEntry.bidPrice = orderEntry.bidPrice - change;
            orderEntry.askPrice = orderEntry.askPrice + change;
        }
        else
        {
            orderEntry.bidPrice = orderEntry.bidPrice + change;
            orderEntry.askPrice = orderEntry.askPrice - change;
        }

        orderEntry.bidSize = 50 + _random.nextInt(150);
        orderEntry.askSize = 50 + _random.nextInt(150);

        // generate match price
        if (orderEntry.bidPrice >= orderEntry.askPrice)
        {
            orderEntry.bidPrice = orderEntry.askPrice;
        }

        long currentTime = System.currentTimeMillis();
        orderEntry.quotim = currentTime % (60 * 60 * 24 * 1000); // need midnight
    }
}
