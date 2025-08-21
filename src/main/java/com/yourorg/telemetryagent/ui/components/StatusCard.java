package com.yourorg.telemetryagent.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * A reusable UI component that displays a metric's title and its current value.
 */
public class StatusCard extends VBox {

    private final Label titleLabel;
    private final Label valueLabel;

    public StatusCard(String title) {
        // Configure the VBox layout
        setPrefWidth(300);   // INCREASED
        setPrefHeight(130);  // INCREASED
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Create and style the title label
        this.titleLabel = new Label(title);
        this.titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;"); // INCREASED

        // Create and style the value label
        this.valueLabel = new Label("--");
        this.valueLabel.setStyle("-fx-font-size: 28px;"); // INCREASED
        this.valueLabel.setTextAlignment(TextAlignment.CENTER);

        // Add the labels to the VBox
        getChildren().addAll(this.titleLabel, this.valueLabel);
    }

    /**
     * Updates the value displayed on the card.
     * @param value The new string value to display.
     */
    public void setValue(String value) {
        this.valueLabel.setText(value);
    }
}