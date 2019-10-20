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
//    private List<RegisteredPlayer> players = new ArrayList<>();

    // nem kell kulon osztaly, meg igy gyorsabb is
    private Map<Player, Boolean> playersSet = new HashMap<>();
    private Cell[] cells;

    private int maxRounds;
    private int roundCounter;

    @Override
    public Player registerPlayer(String name) {
        Player p = new Player(name);
        playersSet.put(p, true);
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

    private boolean validatePlayer(final Command c) {
        Player p = c.getPlayer();
        if (playersSet.containsKey(p) && playersSet.get(p)) {
            playersSet.replace(p, false);
            return true;
        } else {
            return false;
        }
    }

    // megmondja, h az adott parancs ervenyes-e
    // * scan ervenyes, ha jo az intervallum
    // * iro parancsok ervenyesek, ha megfelelo a parameterek szam + erteke
    private boolean validateCommand(Command c, CommandType type) {
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

    private List<MemoryState> getBlockStates(int cellId, Player p) {
        List<MemoryState> states = new ArrayList<>(4);

        for (Cell c : cells)
            if (c.getBlock() == cellId / 4)
                states.add(c.getState(p));

        return states;
    }

    // scan - mindig valid, ha jo az index, de csak a kor vegen fut
    private ResponseScan executeScan(CommandScan scan) {
        if (validateCommand(scan, CommandType.SCAN)) {
            // a blokk elso cellaja
            Integer firstCell = scan.getCell();
            firstCell = firstCell - (firstCell % 4);

            Player p = scan.getPlayer();
            return new ResponseScan(
                p, firstCell, getBlockStates(firstCell, p));
        } else {
            return new ResponseScan(
                scan.getPlayer(), -1, Collections.emptyList());
        }
    }

    // stats - mindig valid, NEM a kor vegen fut!
    private ResponseStats executeStats(CommandStats stats) {
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
    private boolean beginGeneral(
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

    private boolean beginAlloc(CommandAllocate alloc) {
        return beginGeneral(alloc, CommandType.ALLOCATE,
                (cell) -> { cell.allocate(alloc.getPlayer()); });
    }

    private boolean beginFree(CommandFree free) {
        return beginGeneral(free, CommandType.FREE,
                (cell) -> { cell.free(); });
    }

    private boolean beginRecover(CommandRecover rec) {
        return beginGeneral(rec, CommandType.RECOVER,
                (cell) -> { cell.recover(rec.getPlayer()); });
    }

    private boolean beginFortify(CommandFortify fort) {
        return beginGeneral(fort, CommandType.RECOVER,
                (cell) -> { cell.beginFortify(); });
    }

    // swap nem beginGeneral alapu
    private boolean beginSwap(CommandSwap swap) {
        if (validateCommand(swap, CommandType.SWAP)) {
            List<Integer> sw = swap.getCells();

            Integer c1 = sw.get(0);
            Integer c2 = sw.get(1);
            cells[c1].swap2(c2, cells);

            return true;
        } else {
            return false;
        }
    }

    // kozos finalize function, csak a siker feltetelet kell megadni
    private ResponseSuccessList finalizeGeneral(
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

    private ResponseSuccessList finalizeAlloc(CommandAllocate alloc, boolean valid) {
        return finalizeGeneral(alloc, valid,
                (cell) -> {
                    return cell.getOwner() == alloc.getPlayer() &&
                           cell.getState() == MemoryState.ALLOCATED;
                });
    }

    private ResponseSuccessList finalizeFree(CommandFree free, boolean valid) {
        return finalizeGeneral(free, valid,
                (cell) -> {
                    return cell.validWrite() &&
                           cell.getState() == MemoryState.FREE;
                });
    }

    private ResponseSuccessList finalizeRecover(CommandRecover rec, boolean valid) {
        return finalizeGeneral(rec, valid,
                (cell) -> {
                    return cell.validWrite() &&
                           cell.getState() == MemoryState.ALLOCATED;
                });
    }

    private ResponseSuccessList finalizeFortify(CommandFortify fort, boolean valid) {
        return finalizeGeneral(fort, valid,
                (cell) -> {
                    return cell.finishFortify();
                });
    }

    private ResponseSuccessList finalizeSwap(CommandSwap swap, boolean valid) {
        return finalizeGeneral(swap, valid,
                (cell) -> {
                    return cell.successfulySwapped();
                });
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

        //TODO use iterator to reset player status
        for (Map.Entry<Player, Boolean> e : playersSet.entrySet())
            e.setValue(true);

        // evaluate commands and respond
        Set<EvaluatedCommand> evaluated = beginExecute(requests);

        return finalizeRespond(evaluated);
    }

    private PlayerScore calculateScore(Player p) {
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

    // a kiertekelesek eltarolasa (+ HasSet olvasas)
    // egy kicsit felgyorsitja a dolgot, meg jobban olvashato is lesz
    private class EvaluatedCommand {
        final CommandType type;
        final Command command;
        public boolean isValid;

        public EvaluatedCommand(Command command) {
            this.command = command;
            this.type = getCommandType(command);
        }

        // CommandType-kent adja vissza a parancs tipusat,
        // hogy konnyebben lehessen kezelni
        private CommandType getCommandType(Command c) {
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
    }

    private Set<EvaluatedCommand> beginExecute(Command[] commands) {
        Set<EvaluatedCommand> eval = new HashSet<>();

        for (Command c : commands) {
            if (!validatePlayer(c)) continue;

            EvaluatedCommand e = new EvaluatedCommand(c);
            switch (e.type) {
                case SCAN:
                case STATS:
                    e.isValid = true;
                    break;

                case ALLOCATE:
                    e.isValid = beginAlloc((CommandAllocate)c);
                    break;

                case FREE:
                    e.isValid = beginFree((CommandFree)c);
                    break;

                case RECOVER:
                    e.isValid = beginRecover((CommandRecover)c);
                    break;

                case FORTIFY:
                    e.isValid = beginFortify((CommandFortify)c);
                    break;

                case SWAP:
                    e.isValid = beginSwap((CommandSwap)c);
            }
            eval.add(e);
        }

        return eval;
    }

    private List<Response> finalizeRespond(Set<EvaluatedCommand> evaluated) {
        List<Response> results = new ArrayList<>();

        for (EvaluatedCommand ec : evaluated) {
            Response r = null;
            switch (ec.type) {
                case SCAN:
                    // scan a legvegen, egyelore skip
                    continue;

                case STATS:
                    r = executeStats(
                            (CommandStats)ec.command);
                    break;

                case ALLOCATE:
                    r = finalizeAlloc(
                            (CommandAllocate)ec.command,
                            ec.isValid);
                    break;

                case FREE:
                    r = finalizeFree(
                            (CommandFree)ec.command,
                            ec.isValid);
                    break;

                case RECOVER:
                    r = finalizeRecover(
                            (CommandRecover)ec.command,
                            ec.isValid);
                    break;

                case FORTIFY:
                    r = finalizeFortify(
                            (CommandFortify)ec.command,
                            ec.isValid);
                    break;

                case SWAP:
                    r = finalizeSwap(
                            (CommandSwap)ec.command,
                            ec.isValid);
                    break;
            }
            results.add(r);
        }

        //TODO a scanneknek valahogy minden utan kene jonnie
        for (EvaluatedCommand ec : evaluated)
            if (ec.type == CommandType.SCAN)
                results.add(executeScan((CommandScan)ec.command));

        return results;
    }

    @Override
    public List<PlayerScore> getScores() {
        List<PlayerScore> scores = new ArrayList<>();
        for (Player p : playersSet.keySet()) {
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
