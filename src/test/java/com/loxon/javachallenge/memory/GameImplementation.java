package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Game;
import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.PlayerScore;
import com.loxon.javachallenge.memory.api.communication.commands.*;
import com.loxon.javachallenge.memory.api.communication.general.Command;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GameImplementation implements Game {
    private List<RegisteredPlayer> players = new ArrayList<>();
    private Cell[] cells;

    private int maxRounds;
    private int roundCounter;

    @Override
    public Player registerPlayer(String name) {
        RegisteredPlayer p = new RegisteredPlayer(name);
        players.add(p);
        return p;
    }

    @Override
    public void startGame(List<MemoryState> initialMemory, int rounds) {
        if (rounds <= 0) {
            throw new IllegalArgumentException("Number of rounds must greater than zero.");
        }

        maxRounds = rounds;
        roundCounter = 0;

        cells = new Cell[initialMemory.size()];
        for (int i = 0; i < initialMemory.size(); ++i){
            cells[i] = new Cell(i, initialMemory.get(i), null);;
        }
    }

    // CommandType-kent adja vissza a parancs tipusat,
    // hogy konnyebben lehessen kezelni
    CommandType getCommandType(Command c) {
        Class cls = c.getClass();

        if (cls == CommandAllocate.class) {
            return CommandType.ALLOCATE;
        }
        else if (cls == CommandFortify.class) {
            return CommandType.FORTIFY;
        }
        else if (cls == CommandFree.class) {
            return CommandType.FREE;
        }
        else if (cls == CommandRecover.class) {
            return CommandType.RECOVER;
        }
        else if (cls == CommandScan.class) {
            return CommandType.SCAN;
        }
        else if (cls == CommandStats.class) {
            return CommandType.STATS;
        }
        else if (cls == CommandSwap.class) {
            return CommandType.SWAP;
        }

        return CommandType.UNKNOWN;
    }

    boolean validatePlayer(final Command c) {
        // ignore unregistered players
        if (c.getPlayer().getClass() != RegisteredPlayer.class) {
            return false;
        }

        // check if current player can play
        RegisteredPlayer p = (RegisteredPlayer)c.getPlayer();
        if (!p.canPlay) {
            return false;
        }
        else {
            p.canPlay = false;
        }

        return true;
    }

    // megmondja, h az adott parancs ervenyes-e
    // * scan ervenyes, ha jo az intervallum
    // * iro parancsok ervenyesek, ha megfelelo a parameterek szam + erteke
    boolean validateCommand(Command c, CommandType type) {
        switch (type) {
            case SCAN:
                Integer cell = ((CommandScan)c).getCell();
                return cell != null && cell >= 0 && cell < cells.length;

            case SWAP:
                List<Integer> swapping = ((CommandGeneral)c).getCells();

                // need exactly 2 cells
                if (swapping.size() != 2) {
                    return false;
                }

                // cannot be null or out of range
                for (Integer i : swapping) {
                    if (i == null)
                        return false;

                    if (i < 0 || i >= cells.length)
                        return false;
                }

                break;

            case ALLOCATE:
            case FREE:
            case RECOVER:
            case FORTIFY:
                List<Integer> range = ((CommandGeneral)c).getCells();

                // check for more than 2 ids
                if (range.size() > 2)
                    return false;

                // check if ids are in range
                int block = -1;
                for (Integer i : range) {
                    if (i == null)
                        continue;

                    if (block == -1)
                        block = i / 4;

                    if (i < 0 || i >= cells.length || block != i / 4)
                        return false;
                }
                break;

            case UNKNOWN:
                return false;
        }

        return true;
    }

    Cell[] getBlock(int cellId) {
        Cell[] block = new Cell[4];

        int i = 0;
        for (Cell c : cells)
            if (c.getBlock() == cellId / 4)
                block[i++] = c;

        return block;
    }

    List<MemoryState> getBlockStates(int cellId, Player p) {
        Cell[] cells = getBlock(cellId);

        List<MemoryState> states = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            states.add(cells[i].getState(p));
        }

        return states;
    }

    // scan - mindig valid, ha jo az index, de csak a kor vegen fut
    ResponseScan executeScan(CommandScan scan) {
        if (validateCommand(scan, CommandType.SCAN)) {
            Integer firstCell = scan.getCell();
            firstCell = firstCell - (firstCell % 4);

            Player p = scan.getPlayer();
            return new ResponseScan(
                p, firstCell, getBlockStates(firstCell, p)
            );
        } else {
            return new ResponseScan(
                    scan.getPlayer(), -1, Collections.emptyList()
            );
        }

    }

    // stats - mindig valid, NEM a kor vegen fut!
    ResponseStats executeStats(CommandStats stats) {
        ResponseStats s = new ResponseStats(stats.getPlayer());

        int allocated = 0;
        int corrupt = 0;
        int free = 0;
        int system = 0;
        int fortified = 0;
        int owned = 0;
        for (Cell c : cells) {
            switch (c.getState()) {
                case ALLOCATED:
                    ++allocated;
                    break;

                case CORRUPT:
                    ++corrupt;
                    break;

                case FREE:
                    ++free;
                    break;

                case SYSTEM:
                    ++system;
                    break;

                case FORTIFIED:
                    ++fortified;
                    break;
            }

            if (c.getOwner() == stats.getPlayer()) {
                ++owned;
            }
        }

        s.setAllocatedCells(allocated);
        s.setCellCount(cells.length);
        s.setCorruptCells(corrupt);
        s.setFortifiedCells(fortified);
        s.setFreeCells(free);
        s.setSystemCells(system);
        s.setOwnedCells(owned);
        s.setRemainingRounds(maxRounds - roundCounter);

        return s;
    }

    // alloc, free, recover, fortify 2 fazisban
    // 1., mindenki megmondja melyik cellaval mit akar csinalni
    // 2., ha sikeres (== nem irtak 2x ugyanazt), akkor a cella
    //     id-ja hozza lesz adva a ResponseSuccessList tartalmahoz
    //
    // ezek eleg hasonloak, csak a finalize feltetelek masok kb.

    // kozos begin function, csak a cellan torteno modositas
    // valtozik (tipustol fuggoen)
    boolean beginGeneral(
            CommandGeneral cmd,
            CommandType type,
            Consumer<Cell> beginAction) {

        if (validateCommand(cmd, type)) {
            for (Integer i : cmd.getCells())
                if (i != null)
                    beginAction.accept(cells[i]);

            return true;
        } else {
            return false;
        }
    }

    boolean beginAlloc2(CommandAllocate alloc) {
        return beginGeneral(alloc, CommandType.ALLOCATE,
                (cell) -> { cell.allocate(alloc.getPlayer()); });
    }

    boolean beginFree2(CommandFree free) {
        return beginGeneral(free, CommandType.FREE,
                (cell) -> { cell.free(); });
    }

    boolean beginRecover2(CommandRecover rec) {
        return beginGeneral(rec, CommandType.RECOVER,
                (cell) -> { cell.recover(rec.getPlayer()); });
    }

    boolean beginFortify2(CommandFortify fort) {
        return beginGeneral(fort, CommandType.RECOVER,
                (cell) -> { cell.beginFortify(); });
    }

    boolean beginSwap(CommandSwap swap) {
        if (validateCommand(swap, CommandType.SWAP)) {
            List<Integer> sw = swap.getCells();

            Cell c1 = cells[sw.get(0)];
            Cell c2 = cells[sw.get(1)];

            c1.swap(c2);

            return true;
        } else {
            return false;
        }
    }

    // kozos finalize function, csak a siker feltetelet kell megadni
    ResponseSuccessList finalizeGeneral(
            CommandGeneral c,
            boolean valid,
            Predicate<Cell> successCondition) {

        List<Integer> succ = new ArrayList<>();
        for (Integer i : c.getCells()) {
            if (valid &&
                    i != null &&
                    successCondition.test(cells[i])) {

                succ.add(i);
            }
        }

        return new ResponseSuccessList(c.getPlayer(), succ);
    }

    ResponseSuccessList finalizeAlloc2(CommandAllocate alloc, boolean valid) {
        return finalizeGeneral(alloc, valid,
                (cell) -> {
                    return cell.getOwner() == alloc.getPlayer() &&
                           cell.getState() == MemoryState.ALLOCATED;
                });
    }

    ResponseSuccessList finalizeFree2(CommandFree free, boolean valid) {
        return finalizeGeneral(free, valid,
                (cell) -> {
                    return cell.validWrite() &&
                           cell.getState() == MemoryState.FREE;
                });
    }

    ResponseSuccessList finalizeRecover2(CommandRecover rec, boolean valid) {
        return finalizeGeneral(rec, valid,
                (cell) -> {
                    return cell.validWrite() &&
                           cell.getState() == MemoryState.ALLOCATED;
                });
    }

    ResponseSuccessList finalizeFortify2(CommandFortify fort, boolean valid) {
        return finalizeGeneral(fort, valid,
                (cell) -> {
                    return cell.finishFortify();
                });
    }

    ResponseSuccessList finalizeSwap(CommandSwap swap, boolean valid) {
        return finalizeGeneral(swap, valid,
                (cell) -> {
                    return cell.successfulySwapped();
                });
    }

    // ezeket igazabol kitorolheted, mert nincsenek hasznalva
    @Deprecated
    boolean beginFortify(CommandFortify fort) {
        if (validateCommand(fort, CommandType.FORTIFY)) {
            for (Integer i : fort.getCells())
                if (i != null)
                    cells[i].beginFortify();

            return true;
        }
        else {
            return false;
        }
    }

    @Deprecated
    boolean beginAlloc(CommandAllocate alloc) {
        if (validateCommand(alloc, CommandType.ALLOCATE)) {
            for (Integer i : alloc.getCells())
                if (i != null)
                    cells[i].allocate(alloc.getPlayer());

            return true;
        } else {
            return false;
        }
    }

    @Deprecated
    boolean beginFree(CommandFree free) {
        if(validateCommand(free, CommandType.FREE)) {
            for (Integer i : free.getCells())
                if (i != null)
                    cells[i].free();

            return true;
        } else {
            return false;
        }
    }

    @Deprecated
    boolean beginRecover(CommandRecover rec) {
        if (validateCommand(rec, CommandType.RECOVER)) {
            for (Integer i : rec.getCells())
                if (i != null)
                    cells[i].recover(rec.getPlayer());

            return true;
        }
        else {
            return false;
        }
    }

    @Deprecated
    ResponseSuccessList finalizeAlloc(CommandAllocate alloc, boolean valid) {
        List<Integer> succ = new ArrayList<>();
        for (Integer i : alloc.getCells()) {
            if (valid &&
                    i != null &&
                    cells[i].getOwner() == alloc.getPlayer() &&
                    cells[i].getState() == MemoryState.ALLOCATED) {

                succ.add(i);
            }
        }
        return new ResponseSuccessList(alloc.getPlayer(), succ);
    }

    @Deprecated
    ResponseSuccessList finalizeFree(CommandFree free, boolean valid) {
        List<Integer> succ = new ArrayList<>();
        for (Integer i : free.getCells()) {
            if (valid &&
                    i != null &&
                    cells[i].validWrite() &&
                    cells[i].getState() == MemoryState.FREE) {

                succ.add(i);
            }
        }
        return new ResponseSuccessList(free.getPlayer(), succ);
    }

    @Deprecated
    ResponseSuccessList finalizeRecover(CommandRecover rec, boolean valid) {
        List<Integer> succ = new ArrayList<>();
        for (Integer i : rec.getCells()) {
            if (valid &&
                    i != null &&
                    cells[i].validWrite() &&
                    cells[i].getState() == MemoryState.ALLOCATED) {

                succ.add(i);
            }
        }
        return new ResponseSuccessList(rec.getPlayer(), succ);
    }

    @Deprecated
    ResponseSuccessList finalizeFortify(CommandFortify fort, boolean valid) {
        List<Integer> succ = new ArrayList<>();
        for (Integer i : fort.getCells()) {
            if (valid &&
                    i != null &&
                    cells[i].finishFortify()) {

                succ.add(i);
            }
        }
        return new ResponseSuccessList(fort.getPlayer(), succ);
    }

    @Override
    public List<Response> nextRound(Command... requests) {
        if (roundCounter < maxRounds) {
            ++roundCounter;
        } else {
            System.out.println("Game over.");
            return null;
        }

        // set start of round conditions
        for (Cell c : cells)
            c.resetWrites();

        Cell.clearSwapHistory();

        for (RegisteredPlayer p : players)
            p.canPlay = true;

        // TODO execute rest of the commands
        Map<Command, Boolean> evaluated = beginExecute(requests);

        System.out.println(visualize());

        //TODO check rest of the results
        return finalizeRespond(evaluated);
    }

    PlayerScore calculateScore(RegisteredPlayer p) {
        PlayerScore score = new PlayerScore(p);

        int fortified = 0;
        int ownedCells = 0;
        int ownedBlocks = 0;

        int currentBlock = -1;
        int blockCellsOwned = 0;
        for (Cell c : cells) {

            if (c.getOwner() == p) {
                MemoryState state = c.getState(p);
                if (state == MemoryState.OWNED_ALLOCATED) {
                    ++ownedCells;
                }

                if (state == MemoryState.OWNED_FORTIFIED) {
                    ++ownedCells;
                    ++fortified;
                }

                if (currentBlock == -1) {
                    currentBlock = c.getBlock();
                }

                if (c.getBlock() != currentBlock) {
                    currentBlock = -1;
                    blockCellsOwned = 0;
                } else {
                    ++blockCellsOwned;
                    if (blockCellsOwned == 4) {
                        ++ownedBlocks;
                    }
                }
            }
        }

        score.setFortifiedCells(fortified);
        score.setOwnedCells(ownedCells);
        score.setOwnedBlocks(ownedBlocks);
        score.setTotalScore(ownedCells + 4 * ownedBlocks);
        return score;
    }

    // osszes keres kiertekelese, azaz hogy ervenyesek-e v. nem
    // eredmenyek eltarolasa es az ervenyes parancsok futtatasa (1. fazis)
    Map<Command, Boolean> beginExecute(Command[] commands) {
        Map<Command, Boolean> evaluated = new HashMap<>();
        for (Command c : commands) {
            if (validatePlayer(c)) {
                CommandType cmdType = getCommandType(c);

                switch (cmdType) {
                    case SCAN:
                    case STATS:
                        evaluated.put(c, true);
                        break;

                    case ALLOCATE:
                        evaluated.put(
                            c, beginAlloc2((CommandAllocate)c));
                        break;

                    case FREE:
                        evaluated.put(
                            c, beginFree2((CommandFree)c));
                        break;

                    case RECOVER:
                        evaluated.put(
                            c, beginRecover2((CommandRecover)c));
                        break;

                    case FORTIFY:
                        evaluated.put(
                            c, beginFortify2((CommandFortify)c));
                        break;

                    case SWAP:
                        evaluated.put(
                            c, beginSwap((CommandSwap)c));
                }
            } else {
                System.out.println("Command or player invalid");
            }
        }
        return evaluated;
    }

    // valaszadas a kiertekelt parancsokra (minden parancsra kell valasz)
    // P.S.: a scan csak itt fut le valojaban (de lehet hogy ennek is a leg-
    //       vegere kene tenni a fortify miatt
    List<Response> finalizeRespond(Map<Command, Boolean> evaluated) {
        List<Response> results = new ArrayList<>();
        for (Command c : evaluated.keySet()) {
            CommandType cmdType = getCommandType(c);

            Response r = null;
            switch (cmdType) {
                case SCAN:
                    r = executeScan((CommandScan)c);
                    break;

                case STATS:
                    r = executeStats((CommandStats)c);
                    break;

                case ALLOCATE:
                    r = finalizeAlloc2((CommandAllocate)c, evaluated.get(c));
                    break;

                case FREE:
                    r = finalizeFree2((CommandFree)c, evaluated.get(c));
                    break;

                case RECOVER:
                    r = finalizeRecover2((CommandRecover)c, evaluated.get(c));
                    break;

                case FORTIFY:
                    r = finalizeFortify2((CommandFortify)c, evaluated.get(c));
                    break;

                case SWAP:
                    r = finalizeSwap((CommandSwap)c, evaluated.get(c));
            }

            results.add(r);
        }
        return results;
    }

    @Override
    public List<PlayerScore> getScores() {
        List<PlayerScore> scores = new ArrayList<>();
        for (RegisteredPlayer p : players) {
            scores.add(calculateScore(p));
        }
        return scores;
    }

    @Override
    public String visualize() {
        StringBuilder sb = new StringBuilder();
//        for (Player p : players) {
//            sb.append("Player: " + p.getName() + '\n');
//        }


        sb.append("\n[");
        for (int i = 0; i < cells.length; ++i){
            sb.append(cells[i].toString());
            if (i % 4 == 3) {
                sb.append("]\n[");
            }
        }

        String vis = sb.toString();
        vis = vis.substring(0, vis.length() - 1);
        vis += "\nmax rounds: " + maxRounds + '\n';
        vis += "current round: " + roundCounter;

        return vis;
    }
}
