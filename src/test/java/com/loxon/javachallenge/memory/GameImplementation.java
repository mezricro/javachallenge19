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
import java.util.stream.Collectors;

public class GameImplementation implements Game {
    // nem kell kulon osztaly, meg igy gyorsabb is
    private Map<Player, Boolean> players = new HashMap<>();
    private Cell[] cells;

    private int maxRounds;
    private int roundCounter;

    @Override
    public Player registerPlayer(String name) {
        Player p = new Player(name);
        players.put(p, true);
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
            cells[i] = new Cell(i, initialMemory.get(i));
        }
    }

    private boolean isPlayerValid(Player p) {
        boolean isValid =
                players.containsKey(p) &&
                players.get(p);

        if (isValid) players.replace(p, false);

        return isValid;
    }

    // scan - mindig valid, ha jo az index, de csak a kor vegen fut
    private ResponseScan respondScan(CommandScan scan, boolean isValid) {
        List<MemoryState> states = new ArrayList<>(4);
        Integer firstCell = -1;
        Player p = scan.getPlayer();

        if (isValid) {
            // a blokk elso cellaja
            firstCell = scan.getCell();
            firstCell = firstCell - (firstCell % 4);

            for (Cell c : cells)
                if (c.getBlock() == firstCell / 4)
                    states.add(c.getState(p));

        }

        return new ResponseScan(p, firstCell, states);
    }

    // stats - mindig valid, NEM a kor vegen fut!
    private ResponseStats respondStats(CommandStats stats) {
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
    private void executeGeneral(CommandGeneral cmd, Consumer<Cell> beginAction) {
        for (Integer i : cmd.getCells())
            if (i != null)
                beginAction.accept(cells[i]);
    }

    private void allocate(final CommandAllocate cmd) {
        executeGeneral(cmd, c -> c.allocate(cmd.getPlayer()));
    }

    private void free(final CommandFree cmd) {
        executeGeneral(cmd, c -> c.free());
    }

    private void recover(final CommandRecover cmd) {
        executeGeneral(cmd, c -> c.recover(cmd.getPlayer()));
    }

    private void swap(final CommandSwap cmd) {
        List<Integer> toSwap = cmd.getCells();
        Cell.swap(toSwap.get(0), toSwap.get(1), cells);
    }

    private ResponseSuccessList respondGeneral(
            EvaluatedCommand c, Predicate<Cell> successCondition) {

        CommandGeneral gen = (CommandGeneral)c.command;

        List<Integer> succ = new ArrayList<>(2);
        if (c.isValid) {
            for (Integer i : gen.getCells()) {
                if (i != null &&
                    successCondition.test(cells[i])) {

                    succ.add(i);
                }
            }
        }
        return new ResponseSuccessList(c.getPlayer(), succ);
    }

    private ResponseSuccessList respondAllocRecover(EvaluatedCommand allocRecover) {
        return respondGeneral(allocRecover, cell ->
                cell.getState(allocRecover.getPlayer()) == MemoryState.OWNED_ALLOCATED);
    }

    private ResponseSuccessList respondFree(EvaluatedCommand free) {
        return respondGeneral(free,
                cell -> cell.getState() == MemoryState.FREE);
    }

    private ResponseSuccessList respondFortify(EvaluatedCommand fort) {
        return respondGeneral(fort,
                cell -> cell.fortify());
    }

    private ResponseSuccessList respondSwap(EvaluatedCommand swap) {
        return respondGeneral(swap,
                cell -> cell.successfulySwapped());
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
        players.entrySet().forEach(e -> e.setValue(true));

        // evaluate commands and respond
        Set<EvaluatedCommand> evaluated = evaluate(requests);

        return respond(evaluated);
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

    private class EvaluatedCommand {
        final CommandType type;
        final Command command;
        final boolean isValid;

        public EvaluatedCommand(Command command) {
            this.command = command;
            this.type = GameImplementation.getCommandType(command);
            this.isValid = isCommandValid(command);
        }

        public Player getPlayer() {
            return command.getPlayer();
        }

        private boolean isCommandValid(Command c) {
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
    }

    private static CommandType getCommandType(Command c) {
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

    private Set<EvaluatedCommand> evaluate(Command[] commands) {
        Set<EvaluatedCommand> eval = Arrays
                .stream(commands)
                .filter(cmd -> isPlayerValid(cmd.getPlayer()))
                .map(cmd -> new EvaluatedCommand(cmd))
                .collect(Collectors.toSet());

        for (EvaluatedCommand ev : eval) {
            if (ev.isValid) {
                switch (ev.type) {
                    case ALLOCATE:
                        allocate((CommandAllocate)ev.command);
                        break;

                    case FREE:
                        free((CommandFree)ev.command);
                        break;

                    case RECOVER:
                        recover((CommandRecover)ev.command);
                        break;

                    case SWAP:
                        swap((CommandSwap)ev.command);
                        break;

                    default:
                        break;
                }
            }
        }

        return eval;
    }

    private List<Response> respond(Set<EvaluatedCommand> evaluated) {
        List<Response> results = new ArrayList<>();

        for (EvaluatedCommand ec : evaluated) {
            Response r = null;
            switch (ec.type) {
                case STATS:
                    r = respondStats((CommandStats)ec.command);
                    break;

                case ALLOCATE:
                case RECOVER:
                    r = respondAllocRecover(ec);
                    break;

                case FREE:
                    r = respondFree(ec);
                    break;

                case SWAP:
                    r = respondSwap(ec);
                    break;
            }
            if (r != null) results.add(r);
        }

        //TODO fortify is legyen a vegen
        evaluated.stream()
            .filter(ec -> ec.type == CommandType.FORTIFY)
            .map(ec -> respondFortify(ec))
            .forEach(ec -> results.add(ec));

        //TODO a scanneknek valahogy minden utan kene jonnie
        evaluated.stream()
            .filter(ec -> ec.type == CommandType.SCAN)
            .map(ec -> respondScan((CommandScan)ec.command, ec.isValid))
            .forEach(resp -> results.add(resp));

        return results;
    }

    @Override
    public List<PlayerScore> getScores() {
        return players.keySet().stream()
                .map(player -> calculateScore(player))
                .collect(Collectors.toList());
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
