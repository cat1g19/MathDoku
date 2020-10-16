import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static java.lang.Math.*;

public class MathDoku extends Application {

    public static void main(String[] args) {
        launch(args);
    }
    private int size = 0;
    private Cell[][] cells;
    private GridPane grid = new GridPane();
    private ArrayList<Cage> cages = new ArrayList<Cage>();
    private Cell currentCell = null;
    private boolean showMistakes = false;
    private Label message = new Label();
    private Button undo = new Button("UNDO");
    private Button redo = new Button("REDO");
    private File inputFile = new File("inputFile");
    private BorderPane gameGrid = new BorderPane();
    private int[] cellsFrequency = new int[63];
    private BorderPane centerBorderPane = new BorderPane();
    private HBox topHbox = new HBox(10);
    private RotateTransition rotateTransition = new RotateTransition();

    @Override
    public void start(Stage primaryStage) {
        setUpFontSizeButtons();
        setUpButtons();
        setUpMouseNumbers();
        gameGrid.setTop(new VBox(message,topHbox));

        {
            size = 6;
            cells = new Cell[size][size];
            createGrid();
            createCages(size);
            centerBorderPane.setCenter(grid);
        }

        grid.setPrefSize(400,300);
        gameGrid.setCenter(centerBorderPane);
        //gameGrid.setBottom(message);

        gameGrid.setPadding(new Insets(30,30,30,30));

        Scene scene = new Scene(gameGrid);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void setUpFontSizeButtons(){
        VBox vBox = new VBox(15);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(10,10,10,10));
        vBox.setPrefWidth(80);

        Button smallFont = new Button("Small font");
        smallFont.setMinWidth(vBox.getPrefWidth());
        smallFont.setOnAction(e -> changeFontSize(smallFont.getText()));

        Button mediumFont = new Button("Medium font");
        mediumFont.setMinWidth(vBox.getPrefWidth());
        mediumFont.setOnAction(e -> changeFontSize(mediumFont.getText()));

        Button largeFont = new Button("Large font");
        largeFont.setMinWidth(vBox.getPrefWidth());
        largeFont.setOnAction(e -> changeFontSize(largeFont.getText()));

        vBox.getChildren().addAll(smallFont,mediumFont,largeFont);
        topHbox.getChildren().add(vBox);
        //centerBorderPane.setLeft(vBox);
    }

    public void changeFontSize(String buttonType){
        for (int i=0; i<size; i++)
            for (int j=0; j<size; j++){
                cells[i][j].changeFontSize(buttonType);
            }
    }

    public void setUpMouseNumbers(){
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(10,10,10,10));
        vBox.setAlignment(Pos.CENTER);

        GridPane mouseNumbers = new GridPane();

        for (int j=0; j<8; j++) {
            Button b = new Button(Integer.toString(j + 1));
            b.setOnAction(e -> handleButton(b.getText()));
            mouseNumbers.add(b, j%3, j/3);
        }

        Button backspace = new Button("<=");
        backspace.setOnAction(e -> handleButton(backspace.getText()));
        mouseNumbers.add(backspace, 2,2);

        vBox.getChildren().addAll(mouseNumbers);

        topHbox.getChildren().add(mouseNumbers);
        //centerBorderPane.setRight(vBox);
    }

    public void setUpButtons(){
        HBox buttons = new HBox(20);
        buttons.setPadding(new Insets(10,10,10,10));

        undo.setOnAction(e -> undo());

        redo.setOnAction(e -> redo());

        Button clearBoard = new Button("CLEAR");
        clearBoard.setOnAction(e -> clearBoardAlert());

        Button loadFromFile = new Button("LOAD FILE");
        loadFromFile.setOnAction(e -> loadFromFile());

        Button loadFromInput = new Button("LOAD INPUT");
        loadFromInput.setOnAction(e -> loadFromInput());

        Button showMistakes = new Button("MISTAKES");
        showMistakes.setOnAction(e -> toggleShowMistakes());

        buttons.getChildren().addAll(undo,redo,clearBoard,loadFromFile,loadFromInput,showMistakes);
        buttons.setAlignment(Pos.CENTER);
        buttons.setHgrow(undo, Priority.ALWAYS);
        buttons.setHgrow(redo, Priority.ALWAYS);
        buttons.setHgrow(clearBoard, Priority.ALWAYS);
        buttons.setHgrow(loadFromInput, Priority.ALWAYS);
        buttons.setHgrow(loadFromFile, Priority.ALWAYS);
        buttons.setHgrow(showMistakes, Priority.ALWAYS);

        topHbox.getChildren().add(buttons);
        //gameGrid.setTop(buttons);
    }

    public void loadFromFile(){
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            displayGridFromFile(file);
            currentCell = null;
        }
    }

    public void loadFromInput(){
        TextArea textArea = new TextArea();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Text Input");
        alert.setHeaderText("Write the code for the grid and confirm");

        textArea.textProperty().addListener(((observableValue, oldValue, newValue) -> writeToFile(newValue)));
        alert.getDialogPane().setContent(textArea);
        Optional<ButtonType> buttonType = alert.showAndWait();
        if (buttonType.isPresent() && buttonType.get() == ButtonType.OK) {
            displayGridFromFile(inputFile);
            currentCell = null;
        }
    }

    public void displayGridFromFile(File inputFile){
        message.setText("");
        cellsFrequency = new int[64];
        if (beforeGridCreation(inputFile)) {
            centerBorderPane.getChildren().remove(grid);
            cages.clear();
            currentCell = null;
            cells = new Cell[size][size];
            createGrid();
            createCagesFromFile(inputFile);
            centerBorderPane.setCenter(grid);
        }
    }

    public void writeToFile(String newValue){
        try {
            OutputStream outputStream = new FileOutputStream(inputFile);
            outputStream.write(newValue.getBytes());
        }
        catch (Exception ex){
            System.err.println("File exception");
        }
    }

    public void createCagesFromFile(File file){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (bufferedReader.ready()) {
                String[] splitStrings = bufferedReader.readLine().split(" ");
                cages.add(new Cage(getCellsFromString(splitStrings[1]), splitStrings[0]));
            }
        }catch (Exception e){
            System.err.println("Something went wrong with reading from file");
        }
    }

    public boolean beforeGridCreation(File file) {
        int maximum = 0;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (bufferedReader.ready()) {
                String[] splitStrings = bufferedReader.readLine().split(" ");
                String[] splitCell = splitStrings[1].split(",");
                for (String cell : splitCell) {
                    int value = Integer.parseInt(cell);
                    cellsFrequency[value-1] ++;
                    if (value > maximum) {
                        maximum = value;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Something went wrong with reading from file");
        }
        int newSize = (int) ceil(sqrt(maximum));
        System.out.println(newSize);
        if (cellsAreValid(file, newSize)) {
            this.size = newSize;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean cellsAreValid(File file, int size){
        if (!cellsArePartOfOneCage()){
            message.setText("Cells are part of more than only one cage");
            message.setTextFill(Color.RED);
            return false;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (bufferedReader.ready()){
                String[] splitStrings = bufferedReader.readLine().split(" ");
                String[] splitCell = splitStrings[1].split(",");
                if (!cellsAreAdjacent(splitCell, size)){
                    message.setText("Cells given in one cage are not adjacent");
                    message.setTextFill(Color.RED);
                    return false;
                }
                message.setText("");
            }
        }catch (Exception e){
            System.err.println("Something went wrong with reading from file");
        }
        return true;
    }


    public boolean cellsArePartOfOneCage(){
        for (int i=0; i<size*size; i++){
            if (cellsFrequency[i] > 1)
                return false;
        }
        return true;
    }

    public boolean cellsAreAdjacent(String[] cells, int size){
        int noOfCells = cells.length;
        for (int i=0; i<noOfCells; i++) {
            int value = Integer.parseInt(cells[i]);
            for (int j = i + 1; j < noOfCells; j++) {
                int neighbourValue = Integer.parseInt(cells[j]);
                boolean valid = false;
                if (value == neighbourValue + 1 || value == abs(neighbourValue - 1) || value == neighbourValue + size || value == abs(neighbourValue - size)) {
                    valid = true;
                }
                if (valid == false) {
                    for (int k = 0; k < noOfCells; k++) {
                        int otherNeighbour = Integer.parseInt(cells[k]);
                        if (otherNeighbour != neighbourValue) {
                            if (otherNeighbour == neighbourValue + 1 || otherNeighbour == abs(neighbourValue - 1) || otherNeighbour == neighbourValue + size || otherNeighbour == abs(neighbourValue - size)) {
                                valid = true;
                            }
                        }
                        if (otherNeighbour != value){
                            if (value == otherNeighbour + 1 || value == abs(otherNeighbour - 1) || value == otherNeighbour + size || value == abs(otherNeighbour - size)) {
                                valid = true;
                            }
                        }
                    }
                }
                if (valid == false)
                    return false;
            }
        }
        return true;
    }


    public Cell[] getCellsFromString(String commaSeparatedCells){
        String[] splitCell = commaSeparatedCells.split(",");
        Cell[] cellsInACage = new Cell[splitCell.length];
        int index = 0;
        int value ;
        for (String cell : splitCell){
            value = Integer.parseInt(cell) - 1;
            cellsInACage[index] = cells[value / size][value % size];
            System.out.println(value/size + " " + value%size);
            index ++;
        }
        System.out.println("***********");
        return cellsInACage;
    }

    public void undo(){
        if (currentCell != null) {
            int undo = currentCell.getUndoValue();
            int val = currentCell.getVal();
            currentCell.setTextField(Integer.toString(undo));
            currentCell.setUndoValue(-1);
            currentCell.setRedoValue(val);
            currentCell.checkUndoAndRedo();
        }
    }

    public void redo(){
        if (currentCell != null) {
            int redo = currentCell.getRedoValue();
            int val = currentCell.getVal();
            currentCell.setTextField(Integer.toString(redo));
            currentCell.setUndoValue(val);
            currentCell.setRedoValue(-1);
            currentCell.checkUndoAndRedo();
        }
    }

    public void clearBoardAlert(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to clear the board?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK)
            clearTheBoard();
    }

    public void clearTheBoard(){
        for (int i=0; i<size; i++)
            for (int j=0; j<size; j++){
                cells[i][j].setTextField("");
            }
        message.setText("");
    }

    public void toggleShowMistakes(){
        if (showMistakes) {
            showMistakes = false;
            System.out.println(showMistakes);
            unHighlightAllCells();

        }
        else {
            showMistakes = true;
            System.out.println(showMistakes);
            highlightMistakesCages();
            highlightMistakesRowsAndCols();
        }
    }

    public void unHighlightAllCells(){
        for (int i=0; i<size; i++)
            for (int j=0; j<size; j++) {
                cells[i][j].unHighlightCell("RowsAndCols");
                cells[i][j].unHighlightCell("Cages");
            }
    }


    public boolean isGameWon(){
        if (areCagesCorrect() && areRowsAndColsCorrect())
            return true;
        return false;
    }

    public boolean areCagesCorrect(){
        for (Cage cage : cages) {
            if (!cage.isCageCorrect())
                return false;
        }
        return true;
    }

    public boolean areRowsAndColsCorrect(){
        for (int i=0 ; i<size; i++)
            for (int j=0; j<size; j++){
                for (int r=0; r<size; r++) {
                    if (i != r)
                        if (cells[i][j].getVal() == cells[r][j].getVal()) {
                            if (cells[i][j].getVal()!=0 && cells[r][j].getVal()!=0)
                                return false;
                        }
                }
                for (int c=0; c<size; c++) {
                    if (j != c)
                        if (cells[i][j].getVal() == cells[i][c].getVal()) {
                            if (cells[i][j].getVal()!=0 && cells[i][c].getVal()!=0)
                                return false;
                        }
                }
            }
        return true;
    }

    public void highlightMistakesRowsAndCols(){
        boolean areMistakes;
        for (int i=0 ; i<size; i++) {
            for (int j = 0; j < size; j++) {
                areMistakes = false;
                for (int c = 0; c < size; c++) {
                    if (j != c)
                        if (cells[i][j].getVal() == cells[i][c].getVal()) {
                            if ((cells[i][j].getVal() != 0) && (cells[i][c].getVal() != 0)) {
                                areMistakes = true;
                            }
                        }
                }
                if (areMistakes) {
                    for (int col = 0; col < size; col++) {
                        cells[i][col].highlightCell("RowsAndCols");
                        //System.out.println("Call highlight cell at cols");
                    }
                }
                areMistakes = false;
                for (int r = 0; r < size; r++) {
                    if (i != r)
                        if (cells[i][j].getVal() == cells[r][j].getVal()) {
                            if ((cells[i][j].getVal() != 0) && (cells[r][i].getVal() != 0)) {
                                areMistakes = true;
                            }
                        }
                }
                if (areMistakes) {
                    for (int row = 0; row < size; row++) {
                        cells[row][j].highlightCell("RowsAndCols");
                        //System.out.println("Call highlight cell at rows");
                    }
                }
            }
        }
    }

    public void highlightMistakesCages(){
        for (Cage cage : cages){
            if (cage.areCellsFilled()) {
                if (!cage.isCageCorrect()) {
                    Cell[] cells = cage.getCells();
                    for (Cell cell : cells)
                        cell.highlightCell("Cages");
                    }
            }
        }
    }

    public void unHighlightCorrectRowsAndCols(){
        for (int i=0 ; i<size; i++)
            for (int j=0; j<size; j++) {
                boolean isCorrect = true;
                for (int r = 0; r < size; r++) {
                    if (i != r) {
                        if (cells[i][j].getVal() == cells[r][j].getVal()) {
                            if ((cells[i][j].getVal() != 0) && (cells[r][j].getVal() != 0)) {
                                isCorrect = false;
                            }
                        }
                    }
                }
                if (isCorrect) {
                    for (int row = 0; row < size; row++) {
                        cells[row][j].unHighlightCell("RowsAndCols");
                        //System.out.println("Unhighlighted at cell  " + row + " " + j);
                    }
                }
                isCorrect = true;
                for (int c = 0; c < size; c++) {
                    if (j != c) {
                        if (cells[i][j].getVal() == cells[i][c].getVal()) {
                            if ((cells[i][j].getVal() != 0) && (cells[i][c].getVal() != 0)) {
                                isCorrect = false;
                            }
                        }
                    }
                }
                if (isCorrect) {
                    for (int col = 0; col < size; col++) {
                        cells[i][col].unHighlightCell("RowsAndCols");
                        //System.out.println("Unhighlighted at cell  " + i + " " + col);
                    }
                }
            }
    }

    public void unHighlightCorrectCages(){
        for (Cage cage : cages){
            Cell[] cells = cage.getCells();
            if (!cage.areCellsFilled()){
                for (Cell cell : cells)
                    cell.unHighlightCell("Cages");
            }
            if (cage.areCellsFilled()) {
                if (cage.isCageCorrect()) {
                    for (Cell cell : cells)
                        cell.unHighlightCell("Cages");
                }
            }
        }
    }

    public void handleButton(String buttonValue){
       if (currentCell != null) {
           currentCell.setTextField(buttonValue);
       }
    }

    public void createGrid(){
        GridPane grid = new GridPane();
        for (int i=0 ; i<size; i++)
            for (int j=0; j<size; j++) {
                cells[i][j] = new Cell(i, j);
                grid.add(cells[i][j], j, i);
                grid.setHgrow(cells[i][j], Priority.ALWAYS);
                grid.setVgrow(cells[i][j], Priority.ALWAYS);
            }
        this.grid = grid;
    }

    public void createCages(int size){
        if (size == 6) {
            Cell[] cells0 = new Cell[]{cells[0][0], cells[1][0]};
            cages.add(new Cage(cells0, "11+"));

            Cell[] cells1 = new Cell[]{cells[0][1], cells[0][2]};
            cages.add(new Cage(cells1, "2%"));

            Cell[] cells2 = new Cell[]{cells[0][3], cells[1][3]};
            cages.add(new Cage(cells2, "20x"));

            Cell[] cells3 = new Cell[]{cells[0][4], cells[0][5], cells[1][5], cells[2][5]};
            cages.add(new Cage(cells3, "6x"));

            Cell[] cells4 = new Cell[]{cells[1][1], cells[1][2]};
            cages.add(new Cage(cells4, "3-"));

            Cell[] cells5 = new Cell[]{cells[1][4], cells[2][4]};
            cages.add(new Cage(cells5, "3%"));

            Cell[] cells6 = new Cell[]{cells[2][0], cells[2][1], cells[3][0], cells[3][1]};
            cages.add(new Cage(cells6, "240x"));

            Cell[] cells7 = new Cell[]{cells[2][2], cells[2][3]};
            cages.add(new Cage(cells7, "6x"));

            Cell[] cells8 = new Cell[]{cells[3][2], cells[4][2]};
            cages.add(new Cage(cells8, "6x"));

            Cell[] cells9 = new Cell[]{cells[3][3], cells[4][3], cells[4][4]};
            cages.add(new Cage(cells9, "7+"));

            Cell[] cells10 = new Cell[]{cells[3][4], cells[3][5]};
            cages.add(new Cage(cells10, "30x"));

            Cell[] cells11 = new Cell[]{cells[4][0], cells[4][1]};
            cages.add(new Cage(cells11, "6x"));

            Cell[] cells12 = new Cell[]{cells[4][5], cells[5][5]};
            cages.add(new Cage(cells12, "9+"));

            Cell[] cells13 = new Cell[]{cells[5][0], cells[5][1], cells[5][2]};
            cages.add(new Cage(cells13, "8+"));

            Cell[] cells14 = new Cell[]{cells[5][3], cells[5][4]};
            cages.add(new Cage(cells14, "2%"));
        }
    }

    public boolean isNumeric(String str) {
        if (str == null)
            return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public void checkIfWon(){
        if (isGameWon()) {
            rotateTransition.setDuration(Duration.millis(5000));
            rotateTransition.setNode(grid);
            rotateTransition.setByAngle(360);
            rotateTransition.setCycleCount(5);
            rotateTransition.play();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("WINNER");
            alert.setHeaderText("CONGRATULATIONS!");
            alert.setContentText("YOU WON THE GAME!");

            alert.showAndWait();
            //message.setText("CONGRATS YOU WON");
            //message.setTextFill(Color.GREEN);
        }
        else {
            message.setText("");
        }
    }

    public Cage getCage(Cell cell){
        for (Cage cage :cages){
            Cell[] cells1 = cage.getCells();
            for (Cell cell1 : cells1)
                if (cell1 == cell)
                    return cage;
        }
        return null;
    }

    public class Cell extends BorderPane {
        private Label operation;
        private TextField textField = new TextField();
        private String textFieldStyle;
        private String style;
        private int val = 0;
        private int row;
        private int col;
        private boolean isHighlightedRowsAndCols = false;
        private boolean isHighlightedCages = false;
        private int undoValue = -1;
        private int redoValue = -1;

        public Cell(int row, int col){
            this.row = row;
            this.col = col;
            this.style = "-fx-border-color: black; -fx-background-color: white";
            this.setStyle(style);
            textFieldStyle = "-fx-border-color: white; -fx-font-size: 30; -fx-background-color: white";
            textField.setStyle(textFieldStyle);
            textField.setOnMouseClicked(e -> handleClick());
            textField.textProperty().addListener(((observableValue, oldValue, newValue) -> typeValue(newValue, oldValue)));
            this.setCenter(textField);
            textField.setAlignment(Pos.CENTER_RIGHT);
            this.setOnMouseClicked(e -> handleClick());
        }

        public void checkUndoAndRedo(){
            if (undoValue == -1)
                undo.setDisable(true);
            else
                undo.setDisable(false);

            if (redoValue == -1)
                redo.setDisable(true);
            else
                redo.setDisable(false);
        }

        public void typeBackspace(){
            System.out.println("Enter a valid number");
            if (val != 0) {
                undoValue = val;
            }
            val = 0;
            if (showMistakes) {
                unHighlightCorrectRowsAndCols();
                unHighlightCorrectCages();
                highlightMistakesRowsAndCols();
            }
        }

        public void typeLargerValue(String newValue, String oldValue){
            boolean valid = false;
            int novelValue = Integer.parseInt(Character.toString(newValue.charAt(1)));
            for (int i = 1; i <= size; i++) {
                if (novelValue == i)
                    valid = true;
            }
            if (valid) {
                this.setTextField(Integer.toString(novelValue));
            }
            else
                this.setTextField(oldValue);
            if (showMistakes) {
                showMistakes();
            }
        }

        public boolean checkTypeIsValid(int valuePassed){
            boolean valid = false;
            for (int i = 1; i <= size; i++) {
                if (valuePassed == i)
                    valid = true;
            }
            return valid;
        }

        public void typedCorrect(int valuePassed){
            if (val != 0 && val != valuePassed){
                undoValue = val;
            }
            if (val != valuePassed) {
                val = valuePassed;
            }

            checkIfWon();
            if (showMistakes) {
                showMistakes();
            }
        }

        public void showMistakes(){
            unHighlightCorrectRowsAndCols();
            unHighlightCorrectCages();
            highlightMistakesCages();
            highlightMistakesRowsAndCols();
        }

        public void typeValue(String newValue, String oldValue){
            System.out.println(newValue);

            if (newValue.equals("")) {
                typeBackspace();
            }
            else if (isNumeric(newValue)) {
                int valuePassed = Integer.parseInt(newValue);
                if (valuePassed > 9) {
                    typeLargerValue(newValue, oldValue);
                }
                else {
                    if (!checkTypeIsValid(valuePassed) && newValue != null) {
                        this.setTextField("");
                    } else {
                        typedCorrect(valuePassed);
                    }
                }
            }
            else {
                this.setTextField("");
            }

            checkUndoAndRedo();
        }

        public void setTextField(String number){
            if (number.equals("")){
                textField.setText("");
                if (val != 0) {
                    undoValue = val;
                }
                val = 0;
            }
            else {
                textField.setText(number);
            }
        }

        public int getUndoValue(){
            return undoValue;
        }

        public int getRedoValue(){
            return redoValue;
        }

        public void setUndoValue(int number){
            undoValue = number;
        }

        public void setRedoValue(int number){
            redoValue = number;
        }

        public int getVal(){
            return val;
        }

        public void handleClick(){
            currentCell = this;
            checkUndoAndRedo();
            System.out.println("You clicked cell " + row + " " + col);
        }

        public void setBorders(String style){
            this.style = style;
            this.setStyle(style);
        }

        public void setOperation(String operation1){
            operation = new Label(operation1);
            operation.setStyle("-fx-font-size: 17");
            this.setLeft(operation);
        }

        public void changeFontSize(String buttonType){
            if (buttonType.equals("Small font"))
            {
                if (operation != null) {
                    this.operation.setStyle("-fx-font-size: 10");
                }
                String[] textFieldStyles = textFieldStyle.split(";");
                this.textField.setStyle(textFieldStyles[0] + "; -fx-font-size: 20; " + textFieldStyles[2]);
            }
            if (buttonType.equals("Medium font"))
            {
                if (operation != null) {
                    this.operation.setStyle("-fx-font-size: 14");
                }
                String[] textFieldStyles = textFieldStyle.split(";");
                this.textField.setStyle(textFieldStyles[0] + "; -fx-font-size: 25; " + textFieldStyles[2]);
            }
            if (buttonType.equals("Large font"))
            {
                if (operation != null) {
                    this.operation.setStyle("-fx-font-size: 20");
                }
                String[] textFieldStyles = textFieldStyle.split(";");
                this.textField.setStyle(textFieldStyles[0] + "; -fx-font-size: 30; " + textFieldStyles[2]);
            }
        }

        public int getRow(){
            return row;
        }

        public int getCol() {
            return col;
        }

        public void highlightCell(String cagesOrRaC) {
            if (cagesOrRaC.equals("RowsAndCols")){
                isHighlightedRowsAndCols = true;
                //System.out.println("isHighlightedRowsAndCols true");
            }
            else if (cagesOrRaC.equals("Cages")) {
                isHighlightedCages = true;
                //System.out.println("isHighlightedCages true");
            }
            if (isHighlightedCages || isHighlightedRowsAndCols) {
                String[] styles = style.split(";");
                style = styles[0] + ";" + styles[1] + "; -fx-background-color: red";
                this.setStyle(style);
                textFieldStyle = "-fx-border-color: red; -fx-font-size: 30; -fx-background-color: red";
                textField.setStyle(textFieldStyle);
            }
        }

        public void unHighlightCell(String cagesOrRaC){
            if (cagesOrRaC.equals("RowsAndCols")) {
                isHighlightedRowsAndCols = false;
                //System.out.println("isHighlightedRowsAndCols false");
            }
            if (cagesOrRaC.equals("Cages")) {
                isHighlightedCages = false;
                //System.out.println("isHighlightedCages false");
            }
            if (!isHighlightedRowsAndCols && !isHighlightedCages) {
                String[] styles = style.split(";");
                style = styles[0] + ";" + styles[1] + "; -fx-background-color: white";
                this.setStyle(style);
                textFieldStyle = "-fx-border-color: white; -fx-font-size: 30; -fx-background-color: white";
                textField.setStyle(textFieldStyle);
            }
        }
    }

    public class Cage{
        private Cell[] cells;
        private String operation;

        public Cage(Cell[] cells, String operation){
            this.cells = cells;
            this.operation = operation;
            cells[0].setOperation(operation);
            for (Cell cell : cells)
                setCage(cell);
        }

        public void setCage(Cell cell){
            int col = cell.getCol();
            int row = cell.getRow();
            int top = 4;
            int bottom = 4;
            int left = 4;
            int right = 4;
            for (Cell neighbour : cells){
                if (neighbour != cell) {
                    if (neighbour.getRow() == row && neighbour.getCol() == (col + 1))
                        right = 1;
                    if (neighbour.getRow() == row && neighbour.getCol() == (col - 1))
                        left = 1;
                    if (neighbour.getCol() == col && neighbour.getRow() == (row + 1))
                        bottom = 1;
                    if (neighbour.getCol() == col && neighbour.getRow() == (row - 1))
                        top = 1;
                }
            }
            cell.setBorders("-fx-border-width: " + top + " " + right + " " + bottom + " " + left + " ; -fx-border-color: black ; -fx-background-color: white");
        }

        public String getOperation(String operation){
            String finalString = "";
            char[] chars = new char[operation.length()];
            operation.getChars(0,operation.length(),chars,0);
            for (char ch : chars){
                if (isNumeric(Character.toString(ch)))
                    finalString += ch;
                else {
                    finalString = finalString + " " + ch;
                }
            }
            finalString += " c";
            return finalString;
        }

        public boolean isCageCorrect(){
            String[] whatToDo = getOperation(operation).split(" ");
            int target = Integer.parseInt(whatToDo[0]);
            String op;
            if (whatToDo[1].equals("c")){
                op =" ";
            }
            else {
                op = whatToDo[1];
            }
            int result = 0;
            if (op.equals(" ")){
                result = cells[0].getVal();
            }
            if (op.equals("+")) {
                result = 0;
                for (Cell cell : cells)
                    result += cell.getVal();
            }
            if (op.equals("-")) {
                if (areCellsFilled()) {
                    int[] orderedValues = this.orderValues();
                    result = orderedValues[cells.length - 1];
                    for (int i = (cells.length - 2); i >= 0; i--) {
                        result -= orderedValues[i];
                    }
                }
            }
            if (op.equals("x")) {
                result = 1;
                for (Cell cell : cells)
                    result *= cell.getVal();
            }
            if (op.equals("%") || op.equals("÷")) {
                if (areCellsFilled()) {
                    int[] orderedValues = this.orderValues();
                    result = orderedValues[cells.length - 1];
                    for (int i = (cells.length - 2); i >= 0; i--) {
                        result /= orderedValues[i];
                    }
                }
            }
            if (result == target)
                return true;
            return false;
        }

        public Cell[] getCells(){
            return cells;
        }

        public int[] orderValues(){
            int[] values = new int[cells.length];
            int i = 0;
            for (Cell cell : cells) {
                values[i] = cell.getVal();
                i++;
            }
            Arrays.sort(values);
            return values;
        }

        public boolean areCellsFilled(){
            for (Cell cell : cells){
                if (cell.getVal() == 0)
                    return false;
            }
            return true;
        }
    }
}
