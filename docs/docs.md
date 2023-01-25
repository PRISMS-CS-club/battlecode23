# PRISMS 10 robot

## Memory address format

```text
CCTT XXXX XXYY YYYY
```

- `C`: 2 bit time stamp. Records the time of this information being taken.
- `T`: 2 bit mark indicating the current location's subtype
    - Wells
        - `01`: Ad well
        - `10`: Mn well
        - `11`: Ex well
    - Sky Islands
        - `00`: empty island
        - `01`: island occupied by our team
        - `10`: island occupied by enemy team
- `X`: 6 bit x coordinate
- `Y`: 6 bit y coordinate

Special:

- `0011 1111 1111 1111`: default location

## Shared Memory Allocation

- integer 0-7: position of every well
- integer 8-11: position of every headquarters
- integer 12-47: position of every sky island
- integer 48-51: position of every enemy headquarters
- integer 52-57: positions of intense combat
- integer 63: game and memory status indicator
    - format: `ISSS ____ ____ ____`
    - `I`: Whether memory is initialized, either 0 or 1.
    - `S`: Speculation of map symmetry
      - First digit for reflection over x, second digit for reflection over y, third digit for rotating 180 degree.

## Robot states

### Headquarter

| number                                                                | meaning                      |
|-----------------------------------------------------------------------|------------------------------|
| 0 - `initialRobots.length - 1`                                        | producing the initial robots |
| `initialRobots.length` ~ `initialRobots.length + nextAnchorRound - 1` | producing random objects     |
| `initialRobots.length + nextAnchorRound`                              | producing one anchor         |

### Carrier

| number | meaning                              |
|--------|--------------------------------------|
| 0      | no target, exploring the map         |
| 1      | going from headquarter to well       |
| 2      | going from headquarter to sky island |
| 3      | going back to headquarter            |

To prevent robots from stuck, when an robot stays in state 1, 2, or 3 for more than 450 rounds and not holding an
anchor, it will self-destruct.

### Launcher

| number | meaning                                                                                  |
|--------|------------------------------------------------------------------------------------------|
| 0      | initial state. if cannot find any work, do random movement (move toward random location) |
| 1      | moving toward the destination                                                            |
| 2      | staying in one fixed location without movement                                           |
| 3      | moving around the target with it kept in sight                                           |
| 4      | always do random movement. Do not actively searching for work                            |
| 5      | moving to randomly assigned location, when reached, turn to state 3                      |

Assignment of launchers

| position           | amount |
|--------------------|--------|
| around own base    | 20%    |
| at enemy's base    | 35%    |
| around sky islands | 35%    |
| random moving      | 10%    |

## Grid Weight

Each grid is assigned with a grid weight.

Grid weight is determined by many factors:

- Initially, the weight on each grid is a constant number `GridWeight.INITIAL`.
- Each headquarter will affect the nearby grid weights.
    - A grid `d` Euclidean distance away from our headquarter will decrease weight
      by `GridWeight.HQ - d * GridWeight.HQ_DECAY` if this number is greater than 0.
    - A grid `d` Euclidean distance away from enemy's headquarter will increase weight
      by `GridWeight.HQ - d * GridWeight.HQ_DECAY` if this number is greater than 0.
- Each well will affect the nearby grid weight
    - A grid `d` Euclidean distance away from a well will increase weight
      by `GridWeight.WELL - d * GridWeight.WELL_DECAY` if this number is greater than 0.

Grid weight determines the probability of each step in random moving. When an robot is moving randomly, it will select
one of eight grids around it with grid weight being selection probability.